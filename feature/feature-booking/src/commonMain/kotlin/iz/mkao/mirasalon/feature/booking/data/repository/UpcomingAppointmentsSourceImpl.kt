package iz.mkao.mirasalon.feature.booking.data.repository

import iz.mkao.mirasalon.core.domain.repository.UpcomingAppointmentsSource
import iz.mkao.mirasalon.feature.booking.domain.model.BookingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UpcomingAppointmentsSourceImpl(
    private val bookingRepository: BookingRepository
) : UpcomingAppointmentsSource {
    override fun observeUpcomingAppointmentsCount(): Flow<Int> {
        return bookingRepository.confirmedBookings.map { bookings ->
            bookings.count { it.status == BookingStatus.Confirmed }
        }
    }
}
