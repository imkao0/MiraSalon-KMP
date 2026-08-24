package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.ProductPage
import iz.mkao.mirasalon.core.domain.model.Review
import iz.mkao.mirasalon.core.domain.outcome.Outcome

import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun getCategories(): Outcome<List<ProductCategory>>

    suspend fun getProducts(
        category: String?,
        subCategory: String?,
        page: Int,
        pageSize: Int,
        query: String? = null,
        gender: String? = null
    ): Outcome<ProductPage>

    fun observeProducts(
        category: String?,
        subCategory: String?,
        page: Int,
        pageSize: Int,
        query: String? = null,
        gender: String? = null
    ): Flow<Outcome<ProductPage>>

    suspend fun getReviews(productId: String): Outcome<List<Review>>

    suspend fun submitReview(productId: String, rating: Int, comment: String, userId: String? = null): Outcome<Review>

    suspend fun getProduct(id: String): Outcome<Product>

    suspend fun create(
        name: String,
        description: String,
        price: Double,
        category: String,
        stockQuantity: Int,
        imageUrl: String?,
        discountPercent: Int,
        gender: String? = null
    ): Outcome<Product>

    suspend fun update(
        id: String,
        name: String?,
        description: String?,
        price: Double?,
        category: String?,
        stockQuantity: Int?,
        imageUrl: String?,
        discountPercent: Int?,
        gender: String? = null
    ): Outcome<Product>

    suspend fun delete(id: String): Outcome<Unit>

    // Category Management
    suspend fun createCategory(name: String, imageUrl: String?, description: String?): Outcome<ProductCategory>
    suspend fun updateCategory(id: String, name: String?, imageUrl: String?, description: String?): Outcome<ProductCategory>
    suspend fun deleteCategory(id: String): Outcome<Unit>
}
