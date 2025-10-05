package iz.mkao.mirasalon.presentation.promotions

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.AdminPromotion
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory

data class PromotionsUiState(
    val promotions: List<AdminPromotion> = emptyList(),
    val services: List<Service> = emptyList(),
    val productCategories: List<ProductCategory> = emptyList(),
    val serviceCategories: List<ServiceCategory> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val eventSink: (PromotionsEvent) -> Unit = {}
) : CircuitUiState

/** Circuit UI events for the promotions admin screen. */
sealed interface PromotionsEvent : CircuitUiEvent {
    data class Search(val query: String) : PromotionsEvent
    data class CreatePromotion(val promotion: AdminPromotion) : PromotionsEvent
    data class UpdatePromotion(val promotion: AdminPromotion) : PromotionsEvent
    data class DeletePromotion(val id: String) : PromotionsEvent
    data object ClearAll : PromotionsEvent
    data object Refresh : PromotionsEvent
}
