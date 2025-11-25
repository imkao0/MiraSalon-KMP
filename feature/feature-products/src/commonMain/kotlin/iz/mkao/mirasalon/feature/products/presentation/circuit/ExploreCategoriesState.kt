package iz.mkao.mirasalon.feature.products.presentation.circuit

import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.ProductVariation
import iz.mkao.mirasalon.core.domain.model.Promotion

data class ExploreCategoriesState(
    val categories: List<ProductCategory> = emptyList(),
    val products: List<Product> = emptyList(),
    val promotions: List<Promotion> = emptyList(),
    val selectedCategory: ProductCategory? = null,
    val selectedVariation: ProductVariation? = null, // Default to null to show all products
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val cartItemCount: Int = 0,
    val eventSink: (ExploreCategoriesEvent) -> Unit = {}
) : CircuitUiState
