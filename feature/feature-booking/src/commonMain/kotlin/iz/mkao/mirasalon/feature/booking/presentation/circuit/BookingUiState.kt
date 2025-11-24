package iz.mkao.mirasalon.feature.booking.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.feature.booking.domain.model.BookingSpecialist
import iz.mkao.mirasalon.feature.booking.domain.model.BookingTimeSlot
import iz.mkao.mirasalon.feature.booking.domain.model.ConfirmedBooking
import kotlinx.datetime.LocalDate

data class BookingState(
    val services: List<Service> = emptyList(),
    /** Today .. today+6 (7 days). */
    val days: List<LocalDate> = emptyList(),
    val selectedDate: LocalDate? = null,
    val calendarExpanded: Boolean = false,
    val specialists: List<BookingSpecialist> = emptyList(),
    val selectedSpecialistId: String? = null,
    val timeSlots: List<BookingTimeSlot> = emptyList(),
    val selectedSlot: BookingTimeSlot? = null,
    val isLoadingServices: Boolean = false,
    val isLoadingSpecialists: Boolean = false,
    val isLoadingSlots: Boolean = false,
    val slotError: String? = null,
    val isBooking: Boolean = false,
    val bookingError: String? = null,
    val existingBookings: List<ConfirmedBooking> = emptyList(),
    val datesWithBookings: Set<String> = emptySet(),
    val selectedDateBookings: List<ConfirmedBooking> = emptyList(),
    val sheetExpanded: Boolean = false,
    val bookedAppointment: ConfirmedBooking? = null,
    val checkoutBooking: ConfirmedBooking? = null,
    val showConfirmationDialog: Boolean = false,
    val reminderEnabled: Boolean = false,
    val isSpecialistAutoSelected: Boolean = false,
    val canBook: Boolean = false,
    val totalAmount: Double = 0.0,
    val eventSink: (BookingEvent) -> Unit = {},
) : CircuitUiState

sealed interface BookingEvent : CircuitUiEvent {
    data class DateSelected(val date: LocalDate) : BookingEvent
    data object ToggleCalendar : BookingEvent
    data class SpecialistSelected(val specialistId: String) : BookingEvent
    data class SlotSelected(val slot: BookingTimeSlot) : BookingEvent
    data class SheetExpanded(val expanded: Boolean) : BookingEvent
    data object Book : BookingEvent
    data object Continue : BookingEvent
    data object ConsumeCheckoutBooking : BookingEvent
    data object ConsumeBookedAppointment : BookingEvent
    data class ShowConfirmationDialog(val show: Boolean) : BookingEvent
    data object Back : BookingEvent
    data class ToggleReminder(val enabled: Boolean) : BookingEvent
}
