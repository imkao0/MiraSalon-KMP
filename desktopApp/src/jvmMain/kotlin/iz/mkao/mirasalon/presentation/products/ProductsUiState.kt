package iz.mkao.mirasalon.presentation.products

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Product

data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
    val searchQuery: String = "",
    val isLoadingProducts: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val uploadProgress: Float = 0f,
    val selectedCategory: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val eventSink: (ProductsEvent) -> Unit = {}
) : CircuitUiState {
    val isLoading: Boolean get() = isLoadingProducts || isLoadingCategories
}



/** Circuit UI events for the products admin screen. */
sealed interface ProductsEvent : CircuitUiEvent {
    data class Search(val query: String) : ProductsEvent
    data class CategorySelected(val category: String?) : ProductsEvent
    data class PageChanged(val page: Int) : ProductsEvent
    data class UploadImage(val bytes: ByteArray, val fileName: String, val onResult: (String?) -> Unit) : ProductsEvent
    data object ResetUploadProgress : ProductsEvent
    data class CreateProduct(
        val name: String,
        val description: String,
        val price: Double,
        val category: String,
        val stockQuantity: Int,
        val imageUrl: String?,
        val discountPercent: Int
    ) : ProductsEvent
    data class UpdateProduct(
        val id: String,
        val name: String,
        val description: String,
        val price: Double,
        val category: String,
        val stockQuantity: Int,
        val imageUrl: String?,
        val discountPercent: Int
    ) : ProductsEvent
    data class DeleteProduct(val id: String) : ProductsEvent
    data object Refresh : ProductsEvent
    data class CreateCategory(val name: String, val imageUrl: String?, val description: String?) : ProductsEvent
}
