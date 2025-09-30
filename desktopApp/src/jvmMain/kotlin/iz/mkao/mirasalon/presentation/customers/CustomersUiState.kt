package iz.mkao.mirasalon.presentation.customers

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.AdminOrder
import iz.mkao.mirasalon.core.domain.model.AdminReview
import iz.mkao.mirasalon.core.domain.model.CustomerDetail
import iz.mkao.mirasalon.core.domain.model.CustomerSummary
import iz.mkao.mirasalon.core.domain.model.AppointmentDailyPoint

data class CustomersUiState(
    val customers: List<CustomerSummary> = emptyList(),
    val selectedCustomer: CustomerDetail? = null,
    val showReviews: CustomerDetail? = null,
    val customerReviews: List<AdminReview> = emptyList(),
    val showSales: CustomerDetail? = null,
    val customerSales: List<AdminOrder> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val totalCustomers: Int = 0,
    val newCustomers: Int = 0,
    val returnCustomerRate: Double = 0.0,
    val activityPoints: List<AppointmentDailyPoint> = emptyList(),
    val error: String? = null,
    val eventSink: (CustomersEvent) -> Unit = {}
) : CircuitUiState

/** Circuit UI events for the customers admin screen. */
sealed interface CustomersEvent : CircuitUiEvent {
    data class Search(val query: String) : CustomersEvent
    data class SelectCustomer(val id: String?) : CustomersEvent
    data class ShowReviews(val id: String?) : CustomersEvent
    data class ShowSales(val id: String?) : CustomersEvent
    data class CreateCustomer(val name: String, val email: String) : CustomersEvent
    data class UpdateCustomer(val id: String, val name: String, val email: String, val avatarUrl: String?) : CustomersEvent
    data class DeleteCustomer(val id: String) : CustomersEvent
    data object Export : CustomersEvent
    data object Filter : CustomersEvent
}
