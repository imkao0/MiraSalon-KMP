package iz.mkao.mirasalon.server.domain.promotion

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
import io.ktor.server.routing.route
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.CreatePromotionRequestDto
import iz.mkao.mirasalon.core.network.model.dto.UpdatePromotionRequestDto
import iz.mkao.mirasalon.core.network.model.dto.ValidatePromoRequest
import iz.mkao.mirasalon.server.data.repository.PromotionRepository
import iz.mkao.mirasalon.server.util.AppConfig
import iz.mkao.mirasalon.server.util.ensureAdmin
import iz.mkao.mirasalon.server.util.getUserId
import iz.mkao.mirasalon.server.util.validate
import org.slf4j.LoggerFactory
import java.io.File

private val log = LoggerFactory.getLogger("PromotionRoutes")

fun Route.promotionRoutes(
    promotionRepository: PromotionRepository,
    appConfig: AppConfig
) {
    get("/{id}/image") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val pathResult = promotionRepository.getImagePath(id)
        val path = when (pathResult) {
            is Outcome.Success -> pathResult.data
            else -> null
        } ?: return@get call.respond(HttpStatusCode.NotFound)

        if (path.startsWith("http")) {
            return@get call.respondRedirect(path)
        }

        val cleanPath = path.removePrefix("/uploads/").removePrefix("/")
        val file = File(appConfig.uploadDir, cleanPath)
        if (file.exists()) {
            val contentType = ContentType.fromFilePath(file.name).firstOrNull() ?: ContentType.Image.Any
            call.respond(LocalFileContent(file, contentType))
        } else {
            log.warn("Promotion image file not found: ${file.absolutePath} (from path: $path)")
            call.respond(HttpStatusCode.NotFound)
        }
    }

    authenticate("auth-jwt", optional = true) {
        get("/active") {
            val userId = call.getUserId()
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            
            when (val result = promotionRepository.findActive(page, pageSize, userId)) {
                is Outcome.Success -> {
                    val dtos = result.data.map { it.toDto() }
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = dtos))
                }
                is Outcome.Error -> {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = result.failure.toString()))
                }
                else -> {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Unexpected error"))
                }
            }
        }
    }

    authenticate("auth-jwt") {
        route("") {
            get("/all") {
                call.ensureAdmin()
                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                val query = call.request.queryParameters["query"]

                when (val result = promotionRepository.findAll(page, pageSize, query)) {
                    is Outcome.Success -> {
                        val pagedDto = result.data.let { paged ->
                            PagedResponse(
                                items = paged.items.map { it.toDto() },
                                totalCount = paged.totalCount,
                                page = paged.page,
                                pageSize = paged.pageSize,
                                totalPages = paged.totalPages
                            )
                        }
                        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = pagedDto))
                    }
                    is Outcome.Error -> {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = result.failure.toString()))
                    }
                    else -> {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Unexpected error"))
                    }
                }
            }

            post("") {
                call.ensureAdmin()
                val adminId = call.getUserId()
                val request = call.receive<CreatePromotionRequestDto>()
                validate {
                    requireNotBlank("code", request.code)
                    requireNotBlank("description", request.description)
                    requirePositive("discountValue", request.discountValue)
                }

                when (val result = promotionRepository.create(request, actorId = adminId)) {
                    is Outcome.Success -> {
                        log.info("Admin {} created promotion: {}", adminId, result.data.code)
                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.data.toDto()))
                    }
                    is Outcome.Error -> {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.failure.toString()))
                    }
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }
            }

            put("/{id}") {
                call.ensureAdmin()
                val adminId = call.getUserId()
                val id = call.parameters["id"]
                if (id.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(
                        success = false, error = "Promotion ID required"
                    ))
                    return@put
                }

                val request = call.receive<UpdatePromotionRequestDto>()
                validate {
                    request.discountValue?.let { requirePositive("discountValue", it) }
                    request.code?.let { requireNotBlank("code", it) }
                }

                when (val result = promotionRepository.update(id, request, actorId = adminId)) {
                    is Outcome.Success -> {
                        log.info("Admin {} updated promotion: {}", adminId, id)
                        call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                    }
                    is Outcome.Error -> {
                        val status = if (result.failure is Failure.ClientError) HttpStatusCode.NotFound else HttpStatusCode.BadRequest
                        call.respond(status, ApiResponse<Unit>(success = false, error = result.failure.toString()))
                    }
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }
            }

            delete("/{id}") {
                call.ensureAdmin()
                val adminId = call.getUserId()
                val id = call.parameters["id"]
                if (id.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(
                        success = false, error = "Promotion ID required"
                    ))
                    return@delete
                }

                when (val result = promotionRepository.delete(id, actorId = adminId)) {
                    is Outcome.Success -> {
                        log.info("Admin {} deleted promotion: {}", adminId, id)
                        call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                    }
                    is Outcome.Error -> {
                        val status = if (result.failure is Failure.ClientError) HttpStatusCode.NotFound else HttpStatusCode.InternalServerError
                        call.respond(status, ApiResponse<Unit>(success = false, error = result.failure.toString()))
                    }
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }
            }

            delete("/all") {
                call.ensureAdmin()
                val adminId = call.getUserId()
                when (val result = promotionRepository.deleteAll()) {
                    is Outcome.Success -> {
                        log.warn("Admin {} deleted ALL promotions", adminId)
                        call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                    }
                    is Outcome.Error -> call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = result.failure.toString()))
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }
            }

        }

        post("/validate") {
            val userId = call.getUserId()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )

            val request = call.receive<ValidatePromoRequest>()
            when (val resultOutcome = promotionRepository.validatePromoCode(
                code = request.code,
                cartTotal = request.cartTotal,
                userId = userId,
                serviceIds = request.serviceIds
            )) {
                is Outcome.Success -> {
                    val result = resultOutcome.data
                    if (!result.isValid) {
                        log.warn("Invalid promo validation: ${result.errorMessage} for user {}", userId)
                    } else {
                        log.info("Promo validated successfully for user {}", userId)
                    }
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result))
                }
                is Outcome.Error -> {
                    val status = if (resultOutcome.failure is Failure.ClientError) HttpStatusCode.NotFound else HttpStatusCode.BadRequest
                    call.respond(status, ApiResponse<Unit>(success = false, error = resultOutcome.failure.toString()))
                }
                else -> call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("/used") {
            val userId = call.getUserId()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )

            when (val result = promotionRepository.getUserUsedPromotionIds(userId)) {
                is Outcome.Success -> call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.data))
                is Outcome.Error -> call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = result.failure.toString()))
                else -> call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
