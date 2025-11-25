package iz.mkao.mirasalon.feature.products.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.*
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorProductsApi(private val httpClient: HttpClient) : ProductsApi {

    override suspend fun fetchCategories(): Outcome<List<CategoryDto>> = apiCall {
        httpClient.get(Endpoints.CATEGORIES)
    }

    override suspend fun fetchProducts(
        category: String?,
        subCategory: String?,
        page: Int,
        pageSize: Int,
        query: String?,
        gender: String?
    ): Outcome<ProductPageDto> = apiCall {
        httpClient.get(Endpoints.PRODUCTS) {
            category?.let { parameter("category", it) }
            subCategory?.let { parameter("subCategory", it) }
            query?.let { parameter("query", it) }
            gender?.let { parameter("gender", it) }
            parameter("page", page)
            parameter("pageSize", pageSize)
        }
    }

    override suspend fun fetchProduct(id: String): Outcome<ProductDto> = apiCall {
        httpClient.get(Endpoints.product(id))
    }

    override suspend fun fetchReviews(productId: String): Outcome<List<ReviewDto>> = apiCall {
        httpClient.get(Endpoints.reviews(productId))
    }

    override suspend fun submitReview(productId: String, request: SubmitReviewRequest): Outcome<ReviewDto> = apiCall {
        httpClient.post(Endpoints.reviews(productId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun createProduct(request: CreateProductRequest): Outcome<ProductDto> = apiCall {
        httpClient.post(Endpoints.PRODUCTS) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun updateProduct(id: String, request: UpdateProductRequest): Outcome<ProductDto> = apiCall {
        httpClient.put(Endpoints.product(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun deleteProduct(id: String): Outcome<Unit> = apiCall {
        httpClient.delete(Endpoints.product(id))
    }

    override suspend fun createCategory(request: CreateProductCategoryRequest): Outcome<CategoryDto> = apiCall {
        httpClient.post(Endpoints.CATEGORIES) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun updateCategory(id: String, request: UpdateProductCategoryRequest): Outcome<CategoryDto> = apiCall {
        httpClient.put(Endpoints.category(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun deleteCategory(id: String): Outcome<Unit> = apiCall {
        httpClient.delete(Endpoints.category(id))
    }

    private object Endpoints {
        const val CATEGORIES = "/v1/api/products/categories"
        const val PRODUCTS = "/v1/api/products"
        fun product(id: String) = "/v1/api/products/$id"
        fun category(id: String) = "/v1/api/products/categories/$id"
        fun reviews(productId: String) = "/v1/api/products/$productId/reviews"
    }
}
