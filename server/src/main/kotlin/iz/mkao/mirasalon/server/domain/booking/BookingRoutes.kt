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
import iz.mkao.mirasalon.core.network.model.dto.CancelAppointmentResponse
import iz.mkao.mirasalon.core.network.model.dto.SimpleMessageResponse
import iz.mkao.mirasalon.core.network.model.dto.UpdateReminderRequest
import iz.mkao.mirasalon.core.network.model.dto.CreateAppointmentRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateAppointmentStatusRequest
import iz.mkao.mirasalon.server.data.repository.AppointmentRepository
import iz.mkao.mirasalon.server.data.repository.AppointmentStatus
import iz.mkao.mirasalon.server.data.repository.AppointmentUpdateResult
import iz.mkao.mirasalon.server.data.repository.BookingResult
import iz.mkao.mirasalon.server.data.repository.CancelResult
import iz.mkao.mirasalon.server.error.ForbiddenException
import iz.mkao.mirasalon.server.error.GeneralDomainException
import iz.mkao.mirasalon.server.error.ResourceNotFoundException
import iz.mkao.mirasalon.server.error.ScheduleOverlapException
import iz.mkao.mirasalon.server.error.ServiceUnavailableException
import iz.mkao.mirasalon.server.error.UnauthorizedException
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
                ?: throw GeneralDomainException("Missing appointment ID", HttpStatusCode.BadRequest)

            val booking = appointmentRepository.findById(id)
                ?: throw ResourceNotFoundException("Appointment not found")

            val userId = call.getUserId()
            if (userId != null && booking.userId != userId && !call.isAdmin()) {
                throw ForbiddenException("Access denied")
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
                    throw when (result) {
                        is BookingResult.Error.ScheduleOverlap -> ScheduleOverlapException(result.message)
                        is BookingResult.Error.ServiceUnavailable -> ServiceUnavailableException(result.message)
                        is BookingResult.Error.SpecialistNotFound -> ResourceNotFoundException(result.message)
                        is BookingResult.Error.SpecialistUnqualified -> GeneralDomainException(result.message, HttpStatusCode.BadRequest)
                        is BookingResult.Error.SalonMismatch -> GeneralDomainException(result.message, HttpStatusCode.BadRequest)
                        is BookingResult.Error.LeadTimeViolation -> GeneralDomainException(result.message, HttpStatusCode.BadRequest)
                        is BookingResult.Error.ShiftMismatch -> GeneralDomainException(result.message, HttpStatusCode.BadRequest)
                        is BookingResult.Error.SpecialistAbsent -> ScheduleOverlapException(result.message)
                        is BookingResult.Error.PromoError -> GeneralDomainException(result.message, HttpStatusCode.BadRequest)
                        else -> GeneralDomainException(result.message, HttpStatusCode.BadRequest)
                    }
                }
            }
        }


        put("/{id}/status") {
            val id = call.parameters["id"]
                ?: throw GeneralDomainException("Missing appointment ID", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateAppointmentStatusRequest>()
            validate {
                requireNotBlank("status", request.status)
                requireOneOf(
                    "status", request.status,
                    listOf("CONFIRMED", "COMPLETED", "CANCELLED")
                )
            }

    
            if (!call.isAdmin()) {
                throw ForbiddenException("Only admins can change status")
            }

            // Already validated against the allowed set above; map safely without `!!`.
            val newStatus = AppointmentStatus.fromString(request.status)
                ?: throw GeneralDomainException("Invalid status", HttpStatusCode.BadRequest)
            
            val result = appointmentRepository.updateStatus(id, newStatus)
            when (result) {
                is AppointmentUpdateResult.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.appointment))
                }
                is AppointmentUpdateResult.NotFound -> {
                    throw ResourceNotFoundException("Appointment not found")
                }
                is AppointmentUpdateResult.Error -> {
                    throw GeneralDomainException(result.toString(), HttpStatusCode.BadRequest)
                }
            }
        }


        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw GeneralDomainException("Missing appointment ID", HttpStatusCode.BadRequest)

            val userId = call.getUserId() ?: throw UnauthorizedException("Authentication required")
            val isAdmin = call.isAdmin()

            val result = appointmentRepository.cancel(id, userId, isAdmin)
            when (result) {
                is CancelResult.Success -> {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = CancelAppointmentResponse(id = id, cancelledAt = System.currentTimeMillis())
                        )
                    )
                }
                is CancelResult.NotFound -> throw ResourceNotFoundException("Appointment not found")
                is CancelResult.Unauthorized -> throw ForbiddenException("Access denied")
                is CancelResult.AlreadyCancelled -> call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(success = true, data = SimpleMessageResponse("Already cancelled"))
                )
                is CancelResult.CannotCancelPast -> throw GeneralDomainException("Cannot cancel completed appointments", HttpStatusCode.BadRequest)
                is CancelResult.TooLateToCancel -> throw ForbiddenException("Only admins can cancel bookings within 48 hours of the appointment")
                is CancelResult.DatabaseError -> throw GeneralDomainException("Cancellation failed: ${result.cause}")
            }
        }


        put("/{id}/reminder") {
            val id = call.parameters["id"]
                ?: throw GeneralDomainException("Missing appointment ID", HttpStatusCode.BadRequest)

            val userId = call.getUserId()
                ?: throw UnauthorizedException("Authentication required")

            val request = call.receive<UpdateReminderRequest>()
            validate {
                requireBoolean("enabled", request.enabled)
            }

            val result = appointmentRepository.updateReminderEnabled(id, userId, request.enabled)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = Unit))
            } else {
                throw GeneralDomainException(result.exceptionOrNull()?.message ?: "Update failed", HttpStatusCode.BadRequest)
            }
        }
    }
}
