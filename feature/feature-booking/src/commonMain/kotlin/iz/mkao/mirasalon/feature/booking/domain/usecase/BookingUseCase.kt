package iz.mkao.mirasalon.feature.booking.domain.usecase

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.feature.booking.data.repository.BookingRepository
import iz.mkao.mirasalon.feature.booking.domain.model.BookingSpecialist
import iz.mkao.mirasalon.feature.booking.domain.model.BookingTimeSlot
import iz.mkao.mirasalon.feature.booking.domain.model.ConfirmedBooking

/**
 * Thin use-case facade over [BookingRepository] for the booking flow.
 */
class BookingUseCase(
    private val repository: BookingRepository
) {
    suspend fun loadServices(serviceIds: List<String>): List<Service> =
        repository.getServices(serviceIds)

    suspend fun loadSpecialists(serviceId: String): List<BookingSpecialist> =
        repository.getSpecialistsForService(serviceId)

    suspend fun loadTimeSlots(specialistId: String, date: String, duration: Int? = null): List<BookingTimeSlot> =
        repository.getTimeSlots(specialistId, date, duration)

    suspend fun book(
        specialistId: String,
        salonId: String,
        serviceIds: List<String>,
        dateTime: Long
    ): Result<ConfirmedBooking> = repository.createBooking(specialistId, salonId, serviceIds, dateTime)
}
