package iz.mkao.mirasalon.server.domain.customer

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.UpdateCustomerRequestDto
import iz.mkao.mirasalon.server.data.repository.CustomerRepository
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.util.*
import iz.mkao.mirasalon.server.util.AppConfig
import java.io.File

fun Route.customerRoutes(
    customerRepository: CustomerRepository,
    userRepository: UserRepository,
    appConfig: AppConfig
) {

    authenticate("auth-jwt") {

        // Get customer avatar (public)
        get("/{id}/avatar") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val path = userRepository.getAvatarPath(id) ?: return@get call.respond(HttpStatusCode.NotFound)

            if (path.startsWith("http")) {
                return@get call.respondRedirect(path)
            }

            val cleanPath = path.removePrefix("/uploads/").removePrefix("/")
            val file = File(appConfig.uploadDir, cleanPath)
            if (file.exists()) {
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // ── List customers with pagination and search ──
        get("") {
            call.ensureAdmin()
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            val query = call.request.queryParameters["query"]?.takeIf { it.isNotBlank() }?.trim()?.take(100)

            try {
                val customers = customerRepository.findAllSummaries(page, pageSize, query)
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = customers))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, error = "Failed to fetch customers")
                )
            }
        }

        // ── Create customer ──
        post {
            call.ensureAdmin()
            val request = call.receive<UpdateCustomerRequestDto>()
            // Capture into local vals first (public DTO props can't be smart-cast),
            // then validate — no `!!`, no cross-module smart-cast issues.
            val name = request.name?.trim().orEmpty()
            val email = request.email?.trim().orEmpty()
            if (name.isEmpty() || email.isEmpty()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Name and email are required")
                )
            }
            try {
                val id = customerRepository.create(name, email)
                call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = id))
            } catch (e: Exception) {
                // Do not leak internal DB error details to the client.
                call.application.environment.log.error("Failed to create customer", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, error = "Failed to create customer")
                )
            }
        }

        // ── Get full customer detail ──
        get("/{id}") {
            call.ensureAdmin()
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Customer ID is required")
                )
                return@get
            }

            try {
                val detail = customerRepository.getCustomerDetail(id)
                if (detail == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Customer not found")
                    )
                } else {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = detail))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, error = "Failed to fetch customer details")
                )
            }
        }

        // ── Update customer ──
        put("/{id}") {
            call.ensureAdmin()
            val id = call.parameters["id"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = "ID required")
            )
            val request = call.receive<UpdateCustomerRequestDto>()

            try {
                val updated = customerRepository.update(id, request)
                if (updated) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Customer updated"))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Customer not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, error = e.message)
                )
            }
        }

        // ── Soft delete customer ──
        delete("/{id}") {
            call.ensureAdmin()
            val id = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = "ID required")
            )

            try {
                val deleted = customerRepository.softDelete(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Customer deleted"))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Customer not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, error = e.message)
                )
            }
        }

    }
}
