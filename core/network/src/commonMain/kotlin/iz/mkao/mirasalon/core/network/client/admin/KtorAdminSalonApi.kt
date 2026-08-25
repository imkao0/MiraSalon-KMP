package iz.mkao.mirasalon.core.network.client.admin

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.SalonPaginatedResponseDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateSalonRequest
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorAdminSalonApi(private val httpClient: HttpClient) : AdminSalonApi {

    override suspend fun fetchManagementInfo(page: Int, pageSize: Int): Outcome<SalonPaginatedResponseDto> = apiCall<SalonPaginatedResponseDto> {
        httpClient.get(Endpoints.MANAGEMENT) {
            parameter("page", page)
            parameter("pageSize", pageSize)
        }
    }

    override suspend fun updateSalon(id: String, request: UpdateSalonRequest): Outcome<Unit> = apiCall<Unit> {
        httpClient.put("${Endpoints.MANAGEMENT}/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    private object Endpoints {
        const val MANAGEMENT = "/v1/api/salon/management"
    }
}
