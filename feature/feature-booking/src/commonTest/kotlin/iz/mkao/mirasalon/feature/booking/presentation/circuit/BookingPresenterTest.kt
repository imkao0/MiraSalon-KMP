package iz.mkao.mirasalon.feature.booking.presentation.circuit

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.navigation.BookingRoute
import iz.mkao.mirasalon.feature.booking.data.repository.BookingRepository
import iz.mkao.mirasalon.feature.booking.domain.model.BookingSpecialist
import iz.mkao.mirasalon.feature.booking.domain.model.BookingTimeSlot
import iz.mkao.mirasalon.feature.booking.domain.model.ConfirmedBooking
import iz.mkao.mirasalon.feature.booking.domain.usecase.BookingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BookingPresenterTest {

    private val fakeRepository = FakeBookingRepository()
    private val useCase = BookingUseCase(fakeRepository)
    private val navigator = FakeNavigator(BookingRoute.Booking(serviceIds = listOf("service_1")))

    @Test
    fun presenter_initial_state_loads_data() = runTest {
        val screen = BookingRoute.Booking(serviceIds = listOf("service_1"))
        val presenter = BookingPresenter(screen, useCase, fakeRepository, navigator)

        presenter.test {
            awaitItem() 
            
            val loadedState: BookingState = awaitItem()
            
            assertEquals(1, loadedState.services.size)
            assertEquals("Service 1", loadedState.services[0].name)
            assertNotNull(loadedState.selectedDate)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun presenter_updates_state_on_date_selection() = runTest {
        val screen = BookingRoute.Booking(serviceIds = listOf("service_1"))
        val presenter = BookingPresenter(screen, useCase, fakeRepository, navigator)

        presenter.test {
            // Initial states during data loading
            var currentState: BookingState = awaitItem()
            while (currentState.selectedDate == null) {
                currentState = awaitItem()
            }
            
            val initialDate = currentState.selectedDate
            assertNotNull(initialDate)
            
            val nextDate = initialDate.plus(kotlinx.datetime.DatePeriod(days = 1))
            
            currentState.eventSink(BookingEvent.DateSelected(nextDate))
            
            // Consume states until we find the updated date
            var updatedState = awaitItem()
            while (updatedState.selectedDate != nextDate) {
                updatedState = awaitItem()
            }
            
            assertEquals(nextDate, updatedState.selectedDate)
            
            // Allow any trailing states (e.g. from async slot loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeBookingRepository : BookingRepository {
        override val confirmedBookings = MutableStateFlow<List<ConfirmedBooking>>(emptyList())
        override val latestBooking = MutableStateFlow<ConfirmedBooking?>(null)
        override val remindersEnabled = MutableStateFlow<Map<String, Boolean>>(emptyMap())

        override suspend fun refreshBookings() {}

        override suspend fun getServices(serviceIds: List<String>): List<Service> {
            return serviceIds.map { 
                Service(
                    id = it, 
                    name = "Service ${it.last()}", 
                    price = 50.0, 
                    durationMinutes = 30
                ) 
            }
        }

        override suspend fun getSpecialistsForService(serviceId: String): List<BookingSpecialist> {
            return listOf(BookingSpecialist(id = "spec_1", name = "Specialist 1", role = "Barber", salonId = "salon_1"))
        }

        override suspend fun getTimeSlots(specialistId: String, date: String): List<BookingTimeSlot> {
            return listOf(BookingTimeSlot(startTime = 0, endTime = 0, formattedTime = "10:00", isAvailable = true))
        }

        override suspend fun lockSlot(slotId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getDefaultSalonId(): String = "salon_1"
        override suspend fun createBooking(specialistId: String, salonId: String, serviceIds: List<String>, dateTime: Long, reminderEnabled: Boolean): Result<ConfirmedBooking> = Result.failure(Exception("Not implemented"))
        override suspend fun cancelBooking(id: String): Result<Unit> = Result.success(Unit)
        override fun setReminder(bookingId: String, enabled: Boolean) {}
        override fun getBookingById(id: String): ConfirmedBooking? = null
        override suspend fun submitReview(bookingId: String, rating: Int, comment: String): Result<Unit> = Result.success(Unit)
        override suspend fun updateReminderEnabled(bookingId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
    }
}
