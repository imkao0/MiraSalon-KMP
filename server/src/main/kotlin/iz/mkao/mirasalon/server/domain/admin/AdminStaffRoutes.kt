package iz.mkao.mirasalon.server.domain.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.UpdateSpecialistActiveRequest
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistStatusUpdateResult
import iz.mkao.mirasalon.server.error.ForbiddenException
import iz.mkao.mirasalon.server.error.GeneralDomainException
import iz.mkao.mirasalon.server.error.ResourceNotFoundException
import iz.mkao.mirasalon.server.util.isAdmin

fun Route.adminStaffRoutes(specialistRepository: SpecialistRepository) {
    authenticate("auth-jwt") {
        put("/{id}/active") {
            if (!call.isAdmin()) {
                throw ForbiddenException("Admin access required")
            }
            val id = call.parameters["id"] ?: throw GeneralDomainException("Missing id", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateSpecialistActiveRequest>()

            val result = specialistRepository.updateActiveStatus(id, request.isActive)
            when (result) {
                is SpecialistStatusUpdateResult.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                }
                is SpecialistStatusUpdateResult.NotFound -> {
                    throw ResourceNotFoundException("Specialist not found")
                }
                is SpecialistStatusUpdateResult.Failure -> {
                    throw GeneralDomainException(result.message, HttpStatusCode.BadRequest)
                }
            }
        }
    }
}
