package iz.mkao.mirasalon.server.data.repository

import at.favre.lib.crypto.bcrypt.BCrypt
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.model.dto.AuthResponse
import iz.mkao.mirasalon.core.network.model.dto.RegisterRequest
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import iz.mkao.mirasalon.server.data.tables.UsersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.util.UUID

data class User(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val createdAt: Long,
    val referralCode: String? = null,
    val referredByUserId: String? = null,
    val tokenVersion: Int = 1,
    val avatarUrl: String? = null,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val address: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val allergies: List<String> = emptyList()
)

data class UserProfileDetails(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val avatarUrl: String?,
    val dateOfBirth: String?,
    val gender: String?,
    val createdAt: Long,
    val allergies: List<String>
)

sealed class RegisterResult {
    data class Success(val authResponse: AuthResponse) : RegisterResult()
    data class Failure(val message: String) : RegisterResult()
}

sealed class UpdateProfileResult {
    data class Success(val user: User) : UpdateProfileResult()
    data object NotFound : UpdateProfileResult()
    data class Failure(val message: String) : UpdateProfileResult()
}

sealed class DeleteAccountResult {
    data object Success : DeleteAccountResult()
    data object NotFound : DeleteAccountResult()
    data class Failure(val message: String) : DeleteAccountResult()
}

class UserRepository(
    private val outboxRepository: OutboxRepository
) {

    fun findById(id: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun findByEmail(email: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.email.lowerCase() eq email.lowercase().trim() }
            .map { it.toUser() }
            .singleOrNull()
    }

    fun register(request: RegisterRequest): RegisterResult = transaction {
        val emailLower = request.email.lowercase().trim()
        if (UsersTable.selectAll().where { UsersTable.email.lowerCase() eq emailLower }.any()) {
            return@transaction RegisterResult.Failure("Email already exists")
        }

        val id = UUID.randomUUID().toString()
        val passwordHash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
        val now = System.currentTimeMillis()

        try {
            UsersTable.insert {
                it[UsersTable.id] = id
                it[UsersTable.name] = request.name
                it[UsersTable.email] = emailLower
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.role] = request.role.name
                it[UsersTable.createdAt] = now
                it[UsersTable.avatarUrl] = request.avatarUrl
                it[UsersTable.address] = request.address
                it[UsersTable.referralCode] = request.referralCode
            }
        } catch (e: Exception) {
            LoggerFactory.getLogger(UserRepository::class.java).error("Failed to insert user: ${e.message}", e)
            return@transaction RegisterResult.Failure("Database error: ${e.message}")
        }

        val authResponse = AuthResponse(
            token = "", // Token generated in route
            userId = id,
            email = request.email,
            name = request.name,
            role = request.role,
            avatarUrl = request.avatarUrl,
            address = request.address
        )

        RegisterResult.Success(authResponse)
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }

    fun updateProfile(
        userId: String,
        firstName: String?,
        lastName: String?,
        phone: String?,
        address: String?,
        gender: String?,
        avatarUrl: String?,
        dateOfBirth: String? = null,
        allergies: List<String>? = null
    ): UpdateProfileResult = transaction {
        val updatedRows = UsersTable.update({ UsersTable.id eq userId }) {
            if (firstName != null) it[UsersTable.firstName] = firstName
            if (lastName != null) it[UsersTable.lastName] = lastName
            if (phone != null) it[UsersTable.phone] = phone
            if (address != null) it[UsersTable.address] = address
            if (gender != null) it[UsersTable.gender] = gender
            if (avatarUrl != null) it[UsersTable.avatarUrl] = avatarUrl
            if (dateOfBirth != null) it[UsersTable.dateOfBirth] = dateOfBirth
            if (allergies != null) it[UsersTable.allergies] = allergies
        }

        if (updatedRows > 0) {
            val user = findById(userId)
            if (user != null) {
                val event = DomainEvent.UserProfileUpdated(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    actorId = userId,
                    message = "Profile updated for ${user.name}",
                    userId = userId,
                    userName = user.name,
                    userAvatarUrl = user.avatarUrl
                )
                outboxRepository.save(userId, DomainEventCodec.encode(event))
                UpdateProfileResult.Success(user)
            } else UpdateProfileResult.NotFound
        } else {
            UpdateProfileResult.NotFound
        }
    }

    fun getProfileDetails(id: String): UserProfileDetails? = transaction {
        UsersTable.selectAll().where { UsersTable.id eq id }
            .map {
                UserProfileDetails(
                    id = it[UsersTable.id],
                    name = it[UsersTable.name],
                    email = it[UsersTable.email],
                    phone = it[UsersTable.phone],
                    avatarUrl = it[UsersTable.avatarUrl],
                    dateOfBirth = it[UsersTable.dateOfBirth],
                    gender = it[UsersTable.gender],
                    createdAt = it[UsersTable.createdAt],
                    allergies = it[UsersTable.allergies] ?: emptyList()
                )
            }
            .singleOrNull()
    }

    fun deleteAccount(userId: String): DeleteAccountResult = transaction {
        val updatedRows = UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.isDeleted] = true
            it[UsersTable.isActive] = false
        }

        if (updatedRows > 0) DeleteAccountResult.Success
        else DeleteAccountResult.NotFound
    }

    fun getAvatarPath(id: String): String? = transaction {
        UsersTable.select(UsersTable.avatarUrl)
            .where { UsersTable.id eq id }
            .map { it[UsersTable.avatarUrl] }
            .singleOrNull()
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        name = this[UsersTable.name],
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        role = UserRole.fromString(this[UsersTable.role]),
        createdAt = this[UsersTable.createdAt],
        referralCode = this[UsersTable.referralCode],
        referredByUserId = this[UsersTable.referredByUserId],
        tokenVersion = this[UsersTable.tokenVersion],
        avatarUrl = this[UsersTable.avatarUrl],
        isActive = this[UsersTable.isActive],
        isDeleted = this[UsersTable.isDeleted],
        address = this[UsersTable.address],
        firstName = this[UsersTable.firstName],
        lastName = this[UsersTable.lastName],
        phone = this[UsersTable.phone],
        gender = this[UsersTable.gender],
        allergies = this[UsersTable.allergies] ?: emptyList()
    )
}
