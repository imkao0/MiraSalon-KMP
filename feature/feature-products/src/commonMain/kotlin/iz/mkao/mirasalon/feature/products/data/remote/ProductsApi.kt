package iz.mkao.mirasalon.feature.products.data.remote

import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.network.result.safeApiCall
import iz.mkao.mirasalon.core.network.model.dto.CategoryDto
import iz.mkao.mirasalon.core.network.model.dto.ProductPageDto
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto
import iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ProductsApi(private val httpClient: HttpClient) {

    suspend fun fetchCategories(): NetworkResult<List<CategoryDto>> = safeApiCall {
        httpClient.get(Endpoints.CATEGORIES)
    }

    suspend fun fetchProducts(
        category: String?,
        subCategory: String?,
        page: Int,
        pageSize: Int,
    ): NetworkResult<ProductPageDto> = safeApiCall {
        httpClient.get(Endpoints.PRODUCTS) {
            category?.let { parameter("category", it) }
            subCategory?.let { parameter("subCategory", it) }
            parameter("page", page)
            parameter("pageSize", pageSize)
        }
    }

    suspend fun fetchReviews(productId: String): NetworkResult<List<ReviewDto>> = safeApiCall {
        httpClient.get(Endpoints.reviews(productId))
    }

    suspend fun submitReview(productId: String, request: SubmitReviewRequest): NetworkResult<ReviewDto> = safeApiCall {
        httpClient.post(Endpoints.reviews(productId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    private object Endpoints {
        const val CATEGORIES = "/products/categories"
        const val PRODUCTS = "/products"
        fun reviews(productId: String) = "/products/$productId/reviews"
    }
}
