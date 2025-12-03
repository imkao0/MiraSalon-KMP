package iz.mkao.mirasalon.feature.booking.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.booking.domain.model.ConfirmedBooking

sealed interface EReceiptState : CircuitUiState {
    data object Loading : EReceiptState
    
    data class Error(
        val message: String,
        val eventSink: (EReceiptEvent) -> Unit
    ) : EReceiptState

    data class Success(
        val bookingId: String,
        val booking: ConfirmedBooking,
        val customerEmail: String?,
        val eventSink: (EReceiptEvent) -> Unit
    ) : EReceiptState
}

sealed interface EReceiptEvent : CircuitUiEvent {
    data object CloseClicked : EReceiptEvent
    data object BackClicked : EReceiptEvent
    data object ViewBookingsClicked : EReceiptEvent
}
