package iz.mkao.mirasalon.server.domain.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.UpdateSpecialistActiveRequest
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistStatusUpdateResult
import iz.mkao.mirasalon.server.util.isAdmin

fun Route.adminStaffRoutes(specialistRepository: SpecialistRepository) {
    authenticate("auth-jwt") {
        put("/{id}/active") {
            if (!call.isAdmin()) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, error = "Admin access required")
                )
                return@put
            }
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            val request = call.receive<UpdateSpecialistActiveRequest>()

            val result = specialistRepository.updateActiveStatus(id, request.isActive)
            when (result) {
                is SpecialistStatusUpdateResult.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                }
                is SpecialistStatusUpdateResult.NotFound -> {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Specialist not found")
                    )
                }
                is SpecialistStatusUpdateResult.Failure -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = result.message)
                    )
                }
            }
        }
    }
}
