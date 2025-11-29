package iz.mkao.mirasalon.feature.salon.services.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

data class ServicesState(
    val isLoading: Boolean = false,
    val services: List<Service> = emptyList(),
    val promotions: List<Promotion> = emptyList(),
    val categories: List<ServiceCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val isCategoryFixed: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val error: String? = null,
    val eventSink: (ServicesEvent) -> Unit
) : CircuitUiState

sealed interface ServicesEvent : CircuitUiEvent {
    data class CategorySelected(val categoryId: String?) : ServicesEvent
    data class SearchQueryChanged(val query: String) : ServicesEvent
    data class ServiceClicked(val serviceId: String) : ServicesEvent
    data object BackClicked : ServicesEvent
    data object Retry : ServicesEvent
    data object ToggleSortOrder : ServicesEvent
}
