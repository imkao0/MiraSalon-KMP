package iz.mkao.mirasalon.feature.products.presentation.viewmodel

import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.model.Review

data class ProductsUiState(
    val categories: List<ProductCategory> = emptyList(),
    val selectedCategory: String? = null,
    val subCategories: List<String> = emptyList(),
    val selectedSubCategory: String? = null,
    val products: List<Product> = emptyList(),
    val promotions: List<Promotion> = emptyList(),
    val favouriteProductIds: Set<String> = emptySet(),
    val isLoadingProducts: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreProducts: Boolean = true,
    val currentPage: Int = 0,
    val quantity: Int = 1,
    val cartItemCount: Int = 0,
    val productReviews: Map<String, List<Review>> = emptyMap(),
)
