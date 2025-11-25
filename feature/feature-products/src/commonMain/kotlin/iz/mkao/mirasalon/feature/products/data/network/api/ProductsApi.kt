package iz.mkao.mirasalon.feature.products.data.network.api

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.CategoryDto
import iz.mkao.mirasalon.core.network.model.dto.CreateProductCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.CreateProductRequest
import iz.mkao.mirasalon.core.network.model.dto.ProductDto
import iz.mkao.mirasalon.core.network.model.dto.ProductPageDto
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto
import iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateProductCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateProductRequest

interface ProductsApi {
    suspend fun fetchCategories(): Outcome<List<CategoryDto>>
    
    suspend fun fetchProducts(
        category: String?,
        subCategory: String?,
        page: Int,
        pageSize: Int,
        query: String? = null,
        gender: String? = null
    ): Outcome<ProductPageDto>

    suspend fun fetchProduct(id: String): Outcome<ProductDto>
    
    suspend fun fetchReviews(productId: String): Outcome<List<ReviewDto>>
    
    suspend fun submitReview(productId: String, request: SubmitReviewRequest): Outcome<ReviewDto>

    suspend fun createProduct(request: CreateProductRequest): Outcome<ProductDto>
    suspend fun updateProduct(id: String, request: UpdateProductRequest): Outcome<ProductDto>
    suspend fun deleteProduct(id: String): Outcome<Unit>

    suspend fun createCategory(request: CreateProductCategoryRequest): Outcome<CategoryDto>
    suspend fun updateCategory(id: String, request: UpdateProductCategoryRequest): Outcome<CategoryDto>
    suspend fun deleteCategory(id: String): Outcome<Unit>
}
