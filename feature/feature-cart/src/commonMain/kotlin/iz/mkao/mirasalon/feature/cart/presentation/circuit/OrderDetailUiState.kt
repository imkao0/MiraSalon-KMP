package iz.mkao.mirasalon.feature.cart.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Order

data class OrderDetailState(
    val order: Order? = null,
    val placedAt: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val fromCheckout: Boolean = false,
    val eventSink: (OrderDetailEvent) -> Unit = {}
) : CircuitUiState

sealed interface OrderDetailEvent : CircuitUiEvent {
    data object Back : OrderDetailEvent
    data object Home : OrderDetailEvent
}
