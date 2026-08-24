package iz.mkao.mirasalon.feature.salon.services.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.network.model.dto.ServiceCategoryDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceListResponseDto
import iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.network.result.safeApiCall

class KtorServicesApi(private val httpClient: HttpClient) : ServicesApi {

    override suspend fun fetchCategories(): NetworkResult<List<ServiceCategoryDto>> = safeApiCall {
        httpClient.get("/v1/api/services/categories")
    }

    override suspend fun fetchServices(categoryId: String?, query: String?): NetworkResult<List<ServiceDto>> = safeApiCall {
        httpClient.get("/v1/api/services") {
            parameter("categoryId", categoryId)
            parameter("query", query)
        }
    }

    override suspend fun fetchService(id: String): NetworkResult<ServiceDto> = safeApiCall {
        httpClient.get("/v1/api/services/$id")
    }

    override suspend fun submitReview(serviceId: String, request: SubmitReviewRequest): NetworkResult<ReviewDto> = safeApiCall {
        httpClient.post("/v1/api/reviews") {
            contentType(ContentType.Application.Json)
            setBody(request.copy(targetId = serviceId, targetType = "SERVICE"))
        }
    }
}
