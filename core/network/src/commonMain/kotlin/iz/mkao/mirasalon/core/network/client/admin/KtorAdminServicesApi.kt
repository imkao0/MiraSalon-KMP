package iz.mkao.mirasalon.core.network.client.admin

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

class KtorAdminServicesApi(private val httpClient: HttpClient) : AdminServicesApi {

    override suspend fun fetchCategories(): Outcome<List<ServiceCategoryDto>> = apiCall<List<ServiceCategoryDto>> {
        httpClient.get(Endpoints.CATEGORIES)
    }

    override suspend fun fetchServices(categoryId: String?, query: String?): Outcome<List<ServiceDto>> = apiCall<List<ServiceDto>> {
        httpClient.get(Endpoints.SERVICES) {
            parameter("categoryId", categoryId)
            parameter("query", query)
        }
    }

    override suspend fun createService(request: CreateServiceRequestDto): Outcome<ServiceDto> = apiCall<ServiceDto> {
        httpClient.post(Endpoints.SERVICES) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun updateService(id: String, request: UpdateServiceRequestDto): Outcome<Unit> = apiCall<Unit> {
        httpClient.put(Endpoints.service(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun deleteService(id: String): Outcome<Unit> = apiCall<Unit> {
        httpClient.delete(Endpoints.service(id))
    }

    override suspend fun createCategory(request: CreateServiceCategoryRequest): Outcome<ServiceCategoryDto> = apiCall<ServiceCategoryDto> {
        httpClient.post(Endpoints.CATEGORIES) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun updateCategory(id: String, request: UpdateServiceCategoryRequest): Outcome<Unit> = apiCall<Unit> {
        httpClient.put(Endpoints.category(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun deleteCategory(id: String): Outcome<Unit> = apiCall<Unit> {
        httpClient.delete(Endpoints.category(id))
    }

    private object Endpoints {
        const val SERVICES = "/v1/api/services"
        const val CATEGORIES = "/v1/api/services/categories"
        fun service(id: String) = "/v1/api/services/$id"
        fun category(id: String) = "/v1/api/services/categories/$id"
    }
}
