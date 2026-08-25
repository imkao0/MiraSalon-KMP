package iz.mkao.mirasalon.feature.cart.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.core.domain.model.Order
import kotlin.time.Clock

data class OrderUiModel(
    val order: Order,
    val formattedDate: String,
    val isExpired: Boolean = false,
    val formattedExpiry: String? = null
)

data class OrdersState(
    val isLoading: Boolean,
    val orders: List<OrderUiModel>,
    val errorMessage: String? = null,
    val eventSink: (OrdersEvent) -> Unit
) : CircuitUiState

sealed interface OrdersEvent : CircuitUiEvent {
    data object Back : OrdersEvent
    data object Retry : OrdersEvent
    data class OrderClicked(val orderId: String) : OrdersEvent
    data class RemoveOrder(val orderId: String) : OrdersEvent
}

internal fun Order.toUiModel(): OrderUiModel {
    val now = Clock.System.now().toEpochMilliseconds() / 1000
    val expiry = expiresAt
    val isExpired = expiry != null && expiry < now
    val formattedExpiry = expiry?.let { DateUtils.formatDateFull(it) }
    return OrderUiModel(
        order = this,
        formattedDate = DateUtils.formatDateFull(placedAtMillis / 1000),
        isExpired = isExpired,
        formattedExpiry = formattedExpiry
    )
}
