package iz.mkao.mirasalon.server.domain.booking

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.CreateAppointmentRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateAppointmentStatusRequest
import iz.mkao.mirasalon.server.data.repository.AppointmentRepository
import iz.mkao.mirasalon.server.data.repository.AppointmentStatus
import iz.mkao.mirasalon.server.data.repository.AppointmentUpdateResult
import iz.mkao.mirasalon.server.data.repository.BookingResult
import iz.mkao.mirasalon.server.util.getUserId
import iz.mkao.mirasalon.server.util.isAdmin
import iz.mkao.mirasalon.server.util.validate

fun Route.bookingRoutes(
    appointmentRepository: AppointmentRepository
) {

    authenticate("auth-jwt") {


        get("") {
            val userId = call.getUserId() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, error = "Authentication required")
            )

            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            val statusStr = call.request.queryParameters["status"]
            val specialistId = call.request.queryParameters["specialistId"]
            val query = call.request.queryParameters["query"]
            val dateFrom = call.request.queryParameters["dateFrom"]?.toLongOrNull()
            val dateTo = call.request.queryParameters["dateTo"]?.toLongOrNull()

            val status = statusStr?.let { AppointmentStatus.fromString(it) }

            val result = if (call.isAdmin()) {
                appointmentRepository.findAllPaginated(
                    page = page,
                    pageSize = pageSize,
                    status = status,
                    specialistId = specialistId,
                    query = query,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                )
            } else {
                appointmentRepository.findByUserPaginated(userId, page, pageSize)
            }

            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result))
        }


        get("/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(
                    success = false, error = "Missing appointment ID"
                ))

            val booking = appointmentRepository.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(
                    success = false, error = "Appointment not found"
                ))

            val userId = call.getUserId()
            if (userId != null && booking.userId != userId && !call.isAdmin()) {
                return@get call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(
                    success = false, error = "Access denied"
                ))
            }

            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = booking))
        }


        post("") {
            val userId = call.getUserId()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )

            val request = call.receive<CreateAppointmentRequest>()
            validate {
                requireNotBlank("salonId", request.salonId)
                requireNotBlank("specialistId", request.specialistId)
                requireNonNegative("dateTime", request.dateTime.toDouble())
                if (request.serviceIds.isEmpty()) {
                    addError("At least one service must be selected")
                }
            }

            val result = appointmentRepository.create(
                userId = userId,
                salonId = request.salonId,
                specialistId = request.specialistId,
                dateTimeMillis = request.dateTime,
                serviceIds = request.serviceIds,
                promoCode = request.promoCode
            )

            when (result) {
                is BookingResult.Success -> {
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.appointment))
                }
                is BookingResult.Error -> {
                    val status = when (result) {
                        is BookingResult.Error.ScheduleOverlap -> HttpStatusCode.Conflict
                        is BookingResult.Error.ServiceUnavailable -> HttpStatusCode.Gone
                        is BookingResult.Error.SpecialistNotFound -> HttpStatusCode.NotFound
                        is BookingResult.Error.SpecialistUnqualified -> HttpStatusCode.BadRequest
                        is BookingResult.Error.SalonMismatch -> HttpStatusCode.BadRequest
                        is BookingResult.Error.LeadTimeViolation -> HttpStatusCode.BadRequest
                        is BookingResult.Error.ShiftMismatch -> HttpStatusCode.BadRequest
                        is BookingResult.Error.SpecialistAbsent -> HttpStatusCode.Conflict
                        is BookingResult.Error.PromoError -> HttpStatusCode.BadRequest
                        else -> HttpStatusCode.BadRequest
                    }
                    call.respond(status, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }


        put("/{id}/status") {
            val id = call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(
                    success = false, error = "Missing appointment ID"
                ))

            val request = call.receive<UpdateAppointmentStatusRequest>()
            validate {
                requireNotBlank("status", request.status)
                requireOneOf(
                    "status", request.status,
                    listOf("CONFIRMED", "COMPLETED", "CANCELLED")
                )
            }

    
            if (!call.isAdmin()) {
                return@put call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, error = "Only admins can change status")
                )
            }

            // Already validated against the allowed set above; map safely without `!!`.
            val newStatus = AppointmentStatus.fromString(request.status)
                ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Invalid status")
                )
            val result = appointmentRepository.updateStatus(id, newStatus)
            when (result) {
                is AppointmentUpdateResult.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.appointment))
                }
                is AppointmentUpdateResult.NotFound -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Appointment not found"))
                }
                is AppointmentUpdateResult.Error -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.toString()))
                }
            }
        }


        delete("/{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(
                    success = false, error = "Missing appointment ID"
                ))

            if (!call.isAdmin()) {
                return@delete call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, error = "Only admins can delete bookings")
                )
            }

            val deleted = appointmentRepository.delete(id)
            if (deleted) {
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = mapOf("id" to id, "deletedAt" to System.currentTimeMillis())
                    )
                )
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Appointment not found"))
            }
        }


        put("/{id}/reminder") {
            val id = call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(
                    success = false, error = "Missing appointment ID"
                ))

            val userId = call.getUserId()
                ?: return@put call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )

            val request = call.receive<UpdateReminderRequest>()
            validate {
                requireBoolean("enabled", request.enabled)
            }

            val result = appointmentRepository.updateReminderEnabled(id, userId, request.enabled)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = Unit))
            } else {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.exceptionOrNull()?.message ?: "Update failed"))
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class UpdateReminderRequest(
    val enabled: Boolean
)
