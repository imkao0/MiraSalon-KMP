package iz.mkao.mirasalon.feature.products.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.ProductVariation

sealed interface ExploreCategoriesEvent : CircuitUiEvent {
    data object Back : ExploreCategoriesEvent
    data object Refresh : ExploreCategoriesEvent
    data class CategorySelected(val category: ProductCategory?) : ExploreCategoriesEvent
    data class VariationSelected(val variation: ProductVariation?) : ExploreCategoriesEvent
    data class ProductClicked(val productId: String) : ExploreCategoriesEvent
    data class AddToCart(val productId: String) : ExploreCategoriesEvent
    data object CartClicked : ExploreCategoriesEvent
}
