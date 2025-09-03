package iz.mkao.mirasalon.feature.booking.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.navigation.BookingRoute
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.domain.repository.PaymentMethodRepository
import iz.mkao.mirasalon.feature.booking.data.repository.BookingRepository
import iz.mkao.mirasalon.feature.booking.domain.usecase.BookingUseCase
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class BookingPresenter(
    private val screen: BookingRoute.Booking,
    private val bookingUseCase: BookingUseCase,
    private val repository: BookingRepository,
    private val navigator: Navigator,
) : Presenter<BookingState> {

    @OptIn(ExperimentalTime::class)
    @Composable
    override fun present(): BookingState {
        var state by remember { mutableStateOf(BookingState()) }
        val scope = rememberCoroutineScope()
        val confirmedBookings by repository.confirmedBookings.collectAsState(initial = emptyList())

        fun loadSlots(specialistId: String?, date: LocalDate?) {
            if (date == null) return
            val dateString = date.toString()
            scope.launch {
                state = state.copy(isLoadingSlots = true, slotError = null, timeSlots = emptyList())
                val slots = bookingUseCase.loadTimeSlots(specialistId ?: "any", dateString)
                if (slots.none { it.isAvailable }) {
                    state = state.copy(timeSlots = slots, isLoadingSlots = false, slotError = "All slots are fully booked")
                } else {
                    state = state.copy(timeSlots = slots, isLoadingSlots = false, slotError = null)
                }
            }
        }

        LaunchedEffect(Unit) {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val days = (0 until 14).map { today.plus(DatePeriod(days = it)) }
            state = state.copy(days = days, selectedDate = today)

            val services = bookingUseCase.loadServices(screen.serviceIds)
            val specialists = bookingUseCase.loadSpecialists(screen.serviceIds.firstOrNull() ?: "")

            val resolvedSpecialistId = when {
                screen.specialistId != null -> screen.specialistId
                specialists.size == 1 -> specialists.first().id
                else -> null
            }

            state = state.copy(
                services = services,
                specialists = specialists,
                selectedSpecialistId = resolvedSpecialistId,
                isLoadingServices = false,
                isLoadingSpecialists = false,
                isSpecialistAutoSelected = specialists.size == 1
            )

            repository.refreshBookings()

            if (resolvedSpecialistId != null && state.selectedDate != null) {
                loadSlots(resolvedSpecialistId, state.selectedDate)
            }
        }

        val timeZone = remember { TimeZone.currentSystemDefault() }
        val selectedDateBookings = confirmedBookings.filter { booking ->
            state.selectedDate?.let { selected ->
                bookingDate(booking.dateTime, timeZone) == selected
            } == true
        }

        return state.copy(
            existingBookings = confirmedBookings,
            selectedDateBookings = selectedDateBookings,
            totalAmount = state.services.sumOf { it.price },
            canBook = state.selectedDate != null &&
                state.selectedSlot != null &&
                (state.specialists.isEmpty() || state.selectedSpecialistId != null) &&
                !state.isBooking,
            eventSink = { event ->
                when (event) {
                    is BookingEvent.DateSelected -> {
                        if (state.selectedDate == event.date) {
                            state = state.copy(sheetExpanded = !state.sheetExpanded)
                        } else {
                            state = state.copy(
                                selectedDate = event.date,
                                selectedSlot = null,
                                sheetExpanded = true
                            )
                            loadSlots(state.selectedSpecialistId, event.date)
                        }
                    }
                    is BookingEvent.SheetExpanded -> {
                        state = state.copy(sheetExpanded = event.expanded)
                    }
                    BookingEvent.ToggleCalendar -> {
                        state = state.copy(calendarExpanded = !state.calendarExpanded)
                    }
                    is BookingEvent.SpecialistSelected -> {
                        if (state.selectedSpecialistId != event.specialistId) {
                            state = state.copy(
                                selectedSpecialistId = event.specialistId,
                                selectedSlot = null,
                            )
                            loadSlots(event.specialistId, state.selectedDate)
                        }
                    }
                    is BookingEvent.SlotSelected -> {
                        if (event.slot.isAvailable) {
                            state = state.copy(
                                selectedSlot =
                                    if (state.selectedSlot == event.slot) null else event.slot,
                            )
                        }
                    }
                    is BookingEvent.ShowConfirmationDialog -> {
                        state = state.copy(showConfirmationDialog = event.show)
                    }
                    BookingEvent.Book -> {
                        state = state.copy(showConfirmationDialog = false)
                    }
                    BookingEvent.Continue -> {
                        val slot = state.selectedSlot
                        if (slot != null && state.selectedSpecialistId != null && state.services.isNotEmpty()) {
                            val specialist = state.specialists.find { it.id == state.selectedSpecialistId }
                            navigator.goTo(BookingRoute.AppointmentCheckout(
                                serviceIds = state.services.map { it.id },
                                specialistId = state.selectedSpecialistId!!,
                                salonId = specialist?.salonId ?: "main-salon",
                                dateTime = slot.startTime
                            ))
                        }
                    }
                    BookingEvent.Back -> navigator.pop()
                    is BookingEvent.ToggleReminder -> {
                        state = state.copy(reminderEnabled = event.enabled)
                    }
                    BookingEvent.ConsumeCheckoutBooking -> {
                        state = state.copy(checkoutBooking = null)
                    }
                    BookingEvent.ConsumeBookedAppointment -> {
                        state = state.copy(bookedAppointment = null)
                    }
                }
            },
        )
    }
}

class BookingManualPresenterFactory(
    private val bookingUseCase: BookingUseCase,
    private val repository: BookingRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is BookingRoute.Booking -> BookingPresenter(screen, bookingUseCase, repository, navigator)
            is BookingRoute.AppointmentCheckout -> AppointmentCheckoutPresenter(screen, bookingUseCase, paymentMethodRepository, navigator)
            is BookingRoute.PaymentSuccess -> PaymentSuccessPresenter(screen, repository, navigator)
            is BottomNavKey.Booking -> MyBookingsPresenter(repository, navigator)
            is BookingRoute.EReceipt -> EReceiptPresenter(screen, repository, navigator)
            else -> null
        }
    }
}

private fun bookingDate(epochMillis: Long, timeZone: TimeZone): LocalDate? =
    if (epochMillis <= 0L) {
        null
    } else {
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date
    }
