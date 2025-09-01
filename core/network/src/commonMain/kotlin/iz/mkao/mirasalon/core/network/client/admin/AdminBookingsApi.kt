package iz.mkao.mirasalon.core.network.client.admin

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.*

interface AdminBookingsApi {
    suspend fun fetchBookings(
        status: String? = null,
        specialistId: String? = null,
        query: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null,
        page: Int = 1,
        pageSize: Int = 100
    ): Outcome<PagedResponse<AppointmentDto>>

    suspend fun updateBookingStatus(id: String, status: String): Outcome<AppointmentDto>
    suspend fun deleteBooking(id: String): Outcome<Unit>
    suspend fun createBooking(request: CreateAppointmentRequest): Outcome<AppointmentDto>
}
