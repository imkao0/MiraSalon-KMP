package iz.mkao.mirasalon.feature.products.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent

sealed interface ProductsEvent : CircuitUiEvent {
    data class ProductClicked(val productId: String) : ProductsEvent
    data class CategorySelected(val categoryId: String?) : ProductsEvent
    data object Refresh : ProductsEvent
    data object Back : ProductsEvent
    data class SearchQueryChanged(val query: String) : ProductsEvent
}
