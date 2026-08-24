package iz.mkao.mirasalon.feature.booking.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.booking.domain.model.ConfirmedBooking

sealed interface PaymentSuccessEvent : CircuitUiEvent {
    data object Continue : PaymentSuccessEvent
    data object BackToHome : PaymentSuccessEvent
    data object ViewReceipt : PaymentSuccessEvent
}

data class PaymentSuccessState(
    val booking: ConfirmedBooking? = null,
    val isLoading: Boolean = false,
    val currentTimeMillis: Long = 0L,
    val eventSink: (PaymentSuccessEvent) -> Unit = {}
) : CircuitUiState
