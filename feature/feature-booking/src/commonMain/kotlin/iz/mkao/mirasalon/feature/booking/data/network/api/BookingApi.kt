package iz.mkao.mirasalon.feature.booking.data.network.api

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.*

interface BookingApi {
    suspend fun fetchAvailability(
        specialistId: String,
        date: String
    ): Outcome<SpecialistAvailabilityDto>

    suspend fun fetchSpecialists(): Outcome<List<SpecialistDto>>

    suspend fun fetchServices(
        categoryId: String?,
        query: String?
    ): Outcome<List<ServiceDto>>

    suspend fun fetchSalons(): Outcome<PagedResponse<SalonDto>>

    suspend fun createAppointment(request: CreateAppointmentRequest): Outcome<AppointmentDto>

    /** All bookings for the current user (upcoming, completed and cancelled). */
    suspend fun fetchAppointments(): Outcome<PagedResponse<AppointmentDto>>

    suspend fun cancelAppointment(id: String): Outcome<Unit>

    suspend fun submitReview(bookingId: String, request: SubmitReviewRequest): Outcome<Unit>

    suspend fun updateReminderEnabled(bookingId: String, enabled: Boolean): Outcome<Unit>
}
