package iz.mkao.mirasalon.data.repository

import iz.mkao.mirasalon.core.domain.model.AdminAppointment
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStatus
import iz.mkao.mirasalon.core.domain.model.CreateAppointment
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.BookingsRepository
import iz.mkao.mirasalon.core.network.client.admin.AdminBookingsApi
import iz.mkao.mirasalon.core.network.mapper.admin.toDomain
import iz.mkao.mirasalon.core.network.model.dto.CreateAppointmentRequest

class KtorBookingsRepository(
    private val api: AdminBookingsApi
) : BookingsRepository {

    override suspend fun getAll(
        status: AdminAppointmentStatus?,
        query: String?,
        dateFrom: Long?,
        dateTo: Long?
    ): Outcome<List<AdminAppointment>> {
        return api.fetchBookings(
            status = status?.name,
            query = query,
            dateFrom = dateFrom,
            dateTo = dateTo,
            page = 1,
            pageSize = 100
        ).map { paged ->
            paged.items.map { it.toDomain() }
        }
    }

    override suspend fun updateStatus(id: String, status: AdminAppointmentStatus): Outcome<Unit> {
        return api.updateBookingStatus(id, status.name.uppercase()).map { Unit }
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        return api.deleteBooking(id)
    }

    override suspend fun create(request: CreateAppointment): Outcome<Unit> {
        val dtoRequest = CreateAppointmentRequest(
            salonId = request.salonId,
            specialistId = request.specialistId,
            dateTime = request.dateTime,
            serviceIds = request.serviceIds,
            promoCode = request.promoCode
        )
        return api.createBooking(dtoRequest).map { Unit }
    }
}
