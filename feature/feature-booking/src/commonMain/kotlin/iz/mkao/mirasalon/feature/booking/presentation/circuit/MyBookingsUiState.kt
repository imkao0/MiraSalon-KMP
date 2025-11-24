package iz.mkao.mirasalon.feature.booking.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.booking.domain.model.BookingStatus
import iz.mkao.mirasalon.feature.booking.domain.model.ConfirmedBooking

data class MyBookingsState(
    val bookings: List<ConfirmedBooking> = emptyList(),
    val selectedStatus: BookingStatus = BookingStatus.Confirmed,
    val isLoading: Boolean = false,
    val showReviewSheet: Boolean = false,
    val reviewBookingId: String? = null,
    val onReviewSubmit: (suspend (Int, String) -> Result<Unit>)? = null,
    val currentTimeMillis: Long = 0L,
    val eventSink: (MyBookingsEvent) -> Unit = {}
) : CircuitUiState {
    val filteredBookings: List<ConfirmedBooking>
        get() = bookings.filter { it.status == selectedStatus }
}

sealed interface MyBookingsEvent : CircuitUiEvent {
    data object Back : MyBookingsEvent
    data class TabSelected(val status: BookingStatus) : MyBookingsEvent
    data class ReminderToggled(val id: String, val enabled: Boolean) : MyBookingsEvent
    data class EReceiptClicked(val id: String) : MyBookingsEvent
    data class CancelClicked(val id: String) : MyBookingsEvent
    data class RebookClicked(val booking: ConfirmedBooking) : MyBookingsEvent
    data class AddReviewClicked(val id: String) : MyBookingsEvent
    data object DismissReviewSheet : MyBookingsEvent
    data object Refresh : MyBookingsEvent
}
