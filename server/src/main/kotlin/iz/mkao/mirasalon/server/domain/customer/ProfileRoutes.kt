package iz.mkao.mirasalon.server.domain.customer

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.fromFilePath
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.LocalFileContent
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.server.data.repository.UpdateProfileResult
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.data.tables.UserAddressesTable
import iz.mkao.mirasalon.server.data.tables.UserNotificationPreferencesTable
import iz.mkao.mirasalon.server.storage.StorageService
import iz.mkao.mirasalon.server.util.AppConfig
import iz.mkao.mirasalon.server.util.getUserId
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.io.File
import java.util.UUID


@Serializable
data class UserProfileResponse(
    val id: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val memberSinceEpochSeconds: Long,
    val allergies: List<String> = emptyList(),
)

@Serializable
data class ProfileUpdateBody(
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val allergies: List<String>? = null,
)

@Serializable
data class AddressBody(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val label: String = "OTHER",
    val phoneNumber: String,
    val streetAddress: String,
    val number: String,
    val city: String,
    val state: String,
    val isDefault: Boolean = false,
)

@Serializable
data class NotificationPreferencesBody(
    val pushEnabled: Boolean = true,
    val specialistMessagesEnabled: Boolean = true,
    val bookingRemindersEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
)

@Serializable
data class AvatarUploadBody(val avatarUrl: String)

private val log = org.slf4j.LoggerFactory.getLogger("ProfileRoutes")

private val PHONE_REGEX = Regex("^\\+?[0-9\\s()-]{10,20}$")

fun Route.profileRoutes(
    userRepository: UserRepository,
    storageService: StorageService,
    appConfig: AppConfig
) {
    get("/{id}/avatar") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val path = userRepository.getAvatarPath(id) ?: return@get call.respond(HttpStatusCode.NotFound)

        if (path.startsWith("http")) {
            return@get call.respondRedirect(path)
        }

        val cleanPath = path.removePrefix("/uploads/").removePrefix("/")
        val file = File(appConfig.uploadDir, cleanPath)
        if (file.exists()) {
            val contentType = ContentType.fromFilePath(file.name).firstOrNull() ?: ContentType.Image.Any
            call.respond(LocalFileContent(file, contentType))
        } else {
            log.warn("User avatar file not found: ${file.absolutePath} (id: $id, from path: $path)")
            call.respond(HttpStatusCode.NotFound)
        }
    }

    authenticate("auth-jwt") {

        get {
            val userId = call.getUserId() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, error = "Authentication required")
            )

            val details = userRepository.getProfileDetails(userId)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(success = false, error = "Profile not found")
                )

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = UserProfileResponse(
                        id = details.id,
                        fullName = details.name,
                        email = details.email,
                        phoneNumber = details.phone,
                        avatarUrl = if (details.avatarUrl != null) "/v1/api/profile/${details.id}/avatar?v=${details.avatarUrl.hashCode()}" else null,
                        dateOfBirth = details.dateOfBirth,
                        gender = details.gender,
                        memberSinceEpochSeconds = details.createdAt / 1000,
                        allergies = details.allergies,
                    )
                )
            )
        }

        put {
            val userId = call.getUserId() ?: return@put call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, error = "Authentication required")
            )

            val body = call.receive<ProfileUpdateBody>()

            if (body.phoneNumber != null && !PHONE_REGEX.matches(body.phoneNumber)) {
                return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Invalid phone number format")
                )
            }

            val trimmedName = body.fullName?.trim()
            if (body.fullName != null && trimmedName.isNullOrBlank()) {
                return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Name cannot be empty")
                )
            }

            val nameParts = trimmedName?.split(" ", limit = 2)
            val firstName = nameParts?.firstOrNull()
            val lastName = nameParts?.getOrNull(1)

            val result = userRepository.updateProfile(
                userId = userId,
                firstName = firstName,
                lastName = lastName,
                phone = body.phoneNumber,
                address = null,
                gender = body.gender,
                avatarUrl = null,
            )

            when (result) {
                is UpdateProfileResult.Success -> {
                    val details = userRepository.getProfileDetails(userId)
                        ?: return@put call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(success = false, error = "Profile not found")
                        )
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = UserProfileResponse(
                                id = details.id,
                                fullName = details.name,
                                email = details.email,
                                phoneNumber = details.phone,
                                avatarUrl = if (details.avatarUrl != null) "/v1/api/profile/${details.id}/avatar?v=${details.avatarUrl.hashCode()}" else null,
                                dateOfBirth = details.dateOfBirth,
                                gender = details.gender,
                                memberSinceEpochSeconds = details.createdAt / 1000,
                                allergies = details.allergies,
                            )
                        )
                    )
                }
                is UpdateProfileResult.NotFound -> call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(success = false, error = "Profile not found")
                )
                is UpdateProfileResult.Failure -> call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = result.message)
                )
            }
        }

        post("/avatar") {
            val userId = call.getUserId() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, error = "Authentication required")
            )

            val allowedTypes = setOf("image/jpeg", "image/png", "image/webp")

            try {
                val multipart = call.receiveMultipart()
                var uploadResult: iz.mkao.mirasalon.server.storage.UploadResult? = null

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem && uploadResult == null) {
                        val contentType = part.contentType?.toString() ?: "application/octet-stream"
                        if (contentType in allowedTypes) {
                            val ext = when (contentType) {
                                "image/png" -> "png"
                                "image/webp" -> "webp"
                                else -> "jpg"
                            }
                            val originalName = part.originalFileName ?: "avatar.$ext"
                            uploadResult = storageService.uploadStream(
                                stream = { part.provider() },
                                key = "images/avatars/${UUID.randomUUID()}.$ext",
                                originalFileName = originalName,
                                contentType = contentType,
                            )
                        }
                    }
                    part.release
                }

                val result = uploadResult ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "No valid avatar file provided")
                )

                val update = userRepository.updateProfile(
                    userId = userId,
                    firstName = null,
                    lastName = null,
                    phone = null,
                    address = null,
                    gender = null,
                    avatarUrl = result.url,
                )
                if (update is UpdateProfileResult.NotFound) {
                    return@post call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Profile not found")
                    )
                }

                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = AvatarUploadBody(avatarUrl = result.url)))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, error = "Avatar upload failed")
                )
            }
        }

        route("/addresses") {

            get {
                val userId = call.getUserId() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )
                val addresses = newSuspendedTransaction {
                    UserAddressesTable.selectAll()
                        .where { UserAddressesTable.userId eq userId }
                        .orderBy(UserAddressesTable.isDefault to SortOrder.DESC, UserAddressesTable.createdAt to SortOrder.ASC)
                        .map { row -> row.toAddressBody() }
                }
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = addresses))
            }

            post {
                val userId = call.getUserId() ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )
                val body = call.receive<AddressBody>()
                if (body.phoneNumber.isBlank() || body.streetAddress.isBlank() || body.number.isBlank() || body.city.isBlank() || body.state.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "phoneNumber, streetAddress, number, city and state are required")
                    )
                }

                val created = newSuspendedTransaction {
                    val hasAddresses = UserAddressesTable.selectAll()
                        .where { UserAddressesTable.userId eq userId }
                        .count() > 0
                    val shouldBeDefault = body.isDefault || !hasAddresses
                    if (shouldBeDefault) {
                        UserAddressesTable.update({ UserAddressesTable.userId eq userId }) {
                            it[isDefault] = false
                        }
                    }
                    val id = UUID.randomUUID().toString()
                    UserAddressesTable.insert {
                        it[UserAddressesTable.id] = id
                        it[UserAddressesTable.userId] = userId
                        it[label] = body.label
                        it[firstName] = body.firstName.trim()
                        it[lastName] = body.lastName.trim()
                        it[line1] = body.phoneNumber.trim()
                        it[line2] = body.streetAddress.trim()
                        it[city] = body.city.trim()
                        it[state] = body.state.trim()
                        it[postalCode] = body.number.trim()
                        it[country] = ""
                        it[isDefault] = shouldBeDefault
                        it[createdAt] = System.currentTimeMillis()
                    }
                    UserAddressesTable.selectAll()
                        .where { UserAddressesTable.id eq id }
                        .first()
                        .toAddressBody()
                }
                call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = created))
            }

            put("/{id}") {
                val userId = call.getUserId() ?: return@put call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Address ID is required")
                )
                val body = call.receive<AddressBody>()
                if (body.phoneNumber.isBlank() || body.streetAddress.isBlank() || body.number.isBlank() || body.city.isBlank() || body.state.isBlank()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "phoneNumber, streetAddress, number, city and state are required")
                    )
                }

                val updated = newSuspendedTransaction {
                    if (body.isDefault) {
                        UserAddressesTable.update({ UserAddressesTable.userId eq userId }) {
                            it[isDefault] = false
                        }
                    }
                    UserAddressesTable.update({
                        (UserAddressesTable.id eq id) and (UserAddressesTable.userId eq userId)
                    }) {
                        it[label] = body.label
                        it[firstName] = body.firstName.trim()
                        it[lastName] = body.lastName.trim()
                        it[line1] = body.phoneNumber.trim()
                        it[line2] = body.streetAddress.trim()
                        it[city] = body.city.trim()
                        it[state] = body.state.trim()
                        it[postalCode] = body.number.trim()
                        it[country] = ""
                        it[isDefault] = body.isDefault
                    }
                }

                if (updated == 0) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Address not found")
                    )
                }
                val address = newSuspendedTransaction {
                    UserAddressesTable.selectAll()
                        .where { UserAddressesTable.id eq id }
                        .first()
                        .toAddressBody()
                }
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = address))
            }

            delete("/{id}") {
                val userId = call.getUserId() ?: return@delete call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )
                val id = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Address ID is required")
                )
                val deleted = newSuspendedTransaction {
                    UserAddressesTable.deleteWhere {
                        (UserAddressesTable.id eq id) and (UserAddressesTable.userId eq userId)
                    }
                }
                if (deleted == 0) {
                    return@delete call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Address not found")
                    )
                }
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Address deleted"))
            }

            post("/{id}/default") {
                val userId = call.getUserId() ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )
                val id = call.parameters["id"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Address ID is required")
                )
                val found = newSuspendedTransaction {
                    val exists = UserAddressesTable.selectAll()
                        .where { (UserAddressesTable.id eq id) and (UserAddressesTable.userId eq userId) }
                        .any()
                    if (exists) {
                        UserAddressesTable.update({ UserAddressesTable.userId eq userId }) {
                            it[isDefault] = false
                        }
                        UserAddressesTable.update({ UserAddressesTable.id eq id }) {
                            it[isDefault] = true
                        }
                    }
                    exists
                }
                if (!found) {
                    return@post call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Address not found")
                    )
                }
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Default address updated"))
            }
        }

        route("/notification-preferences") {

            get {
                val userId = call.getUserId() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )
                val prefs = newSuspendedTransaction {
                    UserNotificationPreferencesTable.selectAll()
                        .where { UserNotificationPreferencesTable.userId eq userId }
                        .firstOrNull()
                        ?.let { row ->
                            NotificationPreferencesBody(
                                pushEnabled = row[UserNotificationPreferencesTable.pushEnabled],
                                specialistMessagesEnabled = row[UserNotificationPreferencesTable.specialistMessagesEnabled],
                                bookingRemindersEnabled = row[UserNotificationPreferencesTable.bookingRemindersEnabled],
                                marketingEnabled = row[UserNotificationPreferencesTable.marketingEnabled],
                            )
                        } ?: NotificationPreferencesBody()
                }
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = prefs))
            }

            put {
                val userId = call.getUserId() ?: return@put call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )
                val body = call.receive<NotificationPreferencesBody>()
                newSuspendedTransaction {
                    val exists = UserNotificationPreferencesTable.selectAll()
                        .where { UserNotificationPreferencesTable.userId eq userId }
                        .any()
                    if (exists) {
                        UserNotificationPreferencesTable.update({
                            UserNotificationPreferencesTable.userId eq userId
                        }) {
                            it[pushEnabled] = body.pushEnabled
                            it[specialistMessagesEnabled] = body.specialistMessagesEnabled
                            it[bookingRemindersEnabled] = body.bookingRemindersEnabled
                            it[marketingEnabled] = body.marketingEnabled
                            it[updatedAt] = System.currentTimeMillis()
                        }
                    } else {
                        UserNotificationPreferencesTable.insert {
                            it[UserNotificationPreferencesTable.userId] = userId
                            it[pushEnabled] = body.pushEnabled
                            it[specialistMessagesEnabled] = body.specialistMessagesEnabled
                            it[bookingRemindersEnabled] = body.bookingRemindersEnabled
                            it[marketingEnabled] = body.marketingEnabled
                            it[updatedAt] = System.currentTimeMillis()
                        }
                    }
                }
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = body))
            }
        }
    }
}

private fun org.jetbrains.exposed.sql.ResultRow.toAddressBody() = AddressBody(
    id = this[UserAddressesTable.id],
    firstName = this[UserAddressesTable.firstName],
    lastName = this[UserAddressesTable.lastName],
    label = this[UserAddressesTable.label],
    phoneNumber = this[UserAddressesTable.line1],
    streetAddress = this[UserAddressesTable.line2] ?: "",
    number = this[UserAddressesTable.postalCode],
    city = this[UserAddressesTable.city],
    state = this[UserAddressesTable.state] ?: "",
    isDefault = this[UserAddressesTable.isDefault],
)
