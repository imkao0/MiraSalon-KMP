package iz.mkao.mirasalon.presentation.orders

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.AdminOrder
import iz.mkao.mirasalon.core.domain.model.AdminOrderStatus
import iz.mkao.mirasalon.presentation.components.DateFilter

data class OrdersUiState(
    val orders: List<AdminOrder> = emptyList(),
    val isLoading: Boolean = false,
    val selectedStatus: AdminOrderStatus? = null,
    val searchQuery: String = "",
    val dateFilter: DateFilter = DateFilter.ALL,
    val error: String? = null,
    val eventSink: (OrdersEvent) -> Unit = {}
) : CircuitUiState

/** Circuit UI events for the orders admin screen. */
sealed interface OrdersEvent : CircuitUiEvent {
    data class Search(val query: String) : OrdersEvent
    data class DateFilterChanged(val filter: DateFilter) : OrdersEvent
    data class StatusFilterChanged(val status: AdminOrderStatus?) : OrdersEvent
    data class UpdateOrderStatus(val id: String, val status: AdminOrderStatus) : OrdersEvent
    data class DeleteOrder(val id: String) : OrdersEvent
    data object Refresh : OrdersEvent
}
