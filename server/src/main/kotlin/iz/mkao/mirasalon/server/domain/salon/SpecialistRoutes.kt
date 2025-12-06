package iz.mkao.mirasalon.server.domain.salon

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.LocalFileContent
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.CreateSpecialistRequestDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistShiftDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateSpecialistRequestDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateSpecialistStatusRequest
import iz.mkao.mirasalon.server.data.repository.SpecialistAvailabilityRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistCreateResult
import iz.mkao.mirasalon.server.data.repository.SpecialistFetchResult
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistStatusUpdateResult
import iz.mkao.mirasalon.server.data.repository.SpecialistUpdateResult
import iz.mkao.mirasalon.server.util.AppConfig
import iz.mkao.mirasalon.server.util.ensureAdmin
import iz.mkao.mirasalon.server.util.getUserId
import iz.mkao.mirasalon.server.util.isAdmin
import iz.mkao.mirasalon.server.util.validate
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

private val log = LoggerFactory.getLogger("SpecialistRoutes")

fun Route.specialistRoutes(
    specialistRepository: SpecialistRepository,
    availabilityRepository: SpecialistAvailabilityRepository,
    appConfig: AppConfig
) {
    get("/{id}/avatar") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val path = specialistRepository.getAvatarPath(id) ?: return@get call.respond(HttpStatusCode.NotFound)
        
        if (path.startsWith("http")) {
            return@get call.respondRedirect(path)
        }

        val cleanPath = path.removePrefix("/uploads/").removePrefix("/")
        val file = File(appConfig.uploadDir, cleanPath)
        if (file.exists()) {
            val contentType = ContentType.fromFilePath(file.name).firstOrNull() ?: ContentType.Image.Any
            call.respond(LocalFileContent(file, contentType))
        } else {
            log.warn("Avatar file not found: ${file.absolutePath} (from path: $path)")
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/{id}/gallery/{index}") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val index = call.parameters["index"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
        val path = specialistRepository.getGalleryPath(id, index) ?: return@get call.respond(HttpStatusCode.NotFound)

        if (path.startsWith("http")) {
            return@get call.respondRedirect(path)
        }

        val cleanPath = path.removePrefix("/uploads/").removePrefix("/")
        val file = File(appConfig.uploadDir, cleanPath)
        if (file.exists()) {
            val contentType = ContentType.fromFilePath(file.name).firstOrNull() ?: ContentType.Image.Any
            call.respond(LocalFileContent(file, contentType))
        } else {
            log.warn("Gallery file not found: ${file.absolutePath} (index: $index, from path: $path)")
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/online") {
        val salonId = call.request.queryParameters["salonId"]
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

        val result = specialistRepository.findOnlineBySalon(salonId, page, pageSize)
        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result))
    }

    get("/{id}") {
        val id = call.parameters["id"] ?: return@get call.respond(
            HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Missing ID")
        )

        val result = specialistRepository.findById(id)
        when (result) {
            is SpecialistFetchResult.Success -> {
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.specialist))
            }
            is SpecialistFetchResult.NotFound -> {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Specialist not found"))
            }
        }
    }

    get("/{id}/available-slots") {
        val id = call.parameters["id"] ?: return@get call.respond(
            HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Missing ID")
        )
        val dateParam = call.request.queryParameters["date"]
        val duration = call.request.queryParameters["duration"]?.toIntOrNull() ?: 30

        val dateMillis = try {
            if (dateParam.isNullOrBlank()) {
                System.currentTimeMillis()
            } else {
                val trimmed = dateParam.trim()
                // Try parsing as Long (timestamp) first, then fallback to LocalDate (YYYY-MM-DD)
                trimmed.toLongOrNull() ?: LocalDate.parse(trimmed).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        } catch (e: Exception) {
            log.warn("[SpecialistRoutes] Invalid date parameter: '$dateParam'. Falling back to current time. Error: ${e.message}")
            System.currentTimeMillis()
        }

        try {
            val availability = availabilityRepository.getAvailableSlots(id, dateMillis, duration)
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = availability))
        } catch (e: Exception) {
            log.error("[SpecialistRoutes] Failed to fetch available slots for specialist: $id", e)
            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Internal server error"))
        }
    }

    get("") {
        try {
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            val query = call.request.queryParameters["query"]
            val result = specialistRepository.findAll(null, page, pageSize, query)
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result))
        } catch (e: Exception) {
            log.error("Error fetching specialists", e)
            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Failed to fetch specialists"))
        }
    }

    get("/all") {
        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
        val query = call.request.queryParameters["query"]
        val result = specialistRepository.findAll(null, page, pageSize, query)
        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result))
    }

    authenticate("auth-jwt") {
        post("") {
            call.ensureAdmin()
            val adminId = call.getUserId()
            val request = call.receive<CreateSpecialistRequestDto>()
            validate {
                requireNotBlank("name", request.name)
            }

            log.info("Attempting to create specialist: {} for salon: {}", request.name, request.salonId)
            val result = specialistRepository.create(request)
            when (result) {
                is SpecialistCreateResult.Success -> {
                    log.info("Admin {} created specialist: {}", adminId, result.specialist.id)
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.specialist))
                }
                is SpecialistCreateResult.Failure -> {
                    log.warn("Specialist creation failed: {}", result.message)
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }

        put("/{id}") {
            call.ensureAdmin()
            val adminId = call.getUserId()
            val id = call.parameters["id"] ?: return@put call.respond(
                HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "ID required")
            )
            val request = call.receive<UpdateSpecialistRequestDto>()

            val result = specialistRepository.update(id, request)
            when (result) {
                is SpecialistUpdateResult.Success -> {
                    log.info("Admin {} updated specialist: {}", adminId, id)
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                }
                is SpecialistUpdateResult.NotFound -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Specialist not found"))
                }
                is SpecialistUpdateResult.Failure -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }

        delete("/{id}") {
            call.ensureAdmin()
            val adminId = call.getUserId()
            val id = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "ID required")
            )

            when (specialistRepository.delete(id)) {
                is SpecialistStatusUpdateResult.Success -> {
                    log.info("Admin {} deleted specialist: {}", adminId, id)
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                }
                is SpecialistStatusUpdateResult.NotFound -> call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(success = false, error = "Specialist not found")
                )
                is SpecialistStatusUpdateResult.Failure -> call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Failed to delete specialist")
                )
            }
        }

        put("/{id}/status") {
            call.ensureAdmin()
            val adminId = call.getUserId()
            val id = call.parameters["id"] ?: return@put call.respond(
                HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "ID required")
            )
            val request = call.receive<UpdateSpecialistStatusRequest>()
            val status = request.status

            val result = specialistRepository.updateStatus(id, status)
            when (result) {
                is SpecialistStatusUpdateResult.Success -> {
                    log.info("Admin {} updated status for specialist {}: {}", adminId, id, status)
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                }
                is SpecialistStatusUpdateResult.NotFound -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Specialist not found"))
                }
                is SpecialistStatusUpdateResult.Failure -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }

        get("/{id}/stats") {
            call.ensureAdmin()
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "ID required")
            )
            val stats = specialistRepository.getDailyStats(id)
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = stats))
        }

        post("/{id}/reviews") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest>()
            
            val result = specialistRepository.submitReview(id, request.rating, request.comment)
            when (result) {
                is Outcome.Success -> {
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = "Review submitted"))
                }
                is Outcome.Error -> call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.failure.toString()))
                else -> {}
            }
        }

        post("/{id}/shifts") {
            val id = call.parameters["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "ID required")
            )
            val userId = call.getUserId()
            val specialist = specialistRepository.findById(id)
            val isOwner = specialist is SpecialistFetchResult.Success && specialist.specialist.userId == userId
            
            if (!call.isAdmin() && !isOwner) {
                return@post call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Access denied"))
            }

            val shifts = call.receive<List<SpecialistShiftDto>>()
            val result = specialistRepository.updateShifts(id, shifts)
            when (result) {
                is SpecialistStatusUpdateResult.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                }
                else -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Failed to update shifts"))
                }
            }
        }
    }
}
