package iz.mkao.mirasalon.feature.products.presentation.circuit

import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.ProductCategory

data class ProductsState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<ProductCategory> = emptyList(),
    val products: List<Product> = emptyList(),
    val error: String? = null,
    val eventSink: (ProductsEvent) -> Unit = {},
) : CircuitUiState
