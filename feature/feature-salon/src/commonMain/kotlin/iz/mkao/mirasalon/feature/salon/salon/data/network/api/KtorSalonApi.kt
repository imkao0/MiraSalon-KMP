package iz.mkao.mirasalon.feature.salon.salon.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import iz.mkao.mirasalon.core.network.model.dto.SalonDto
import iz.mkao.mirasalon.core.network.model.dto.SalonHomeDto
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.network.result.safeApiCall

class KtorSalonApi(private val httpClient: HttpClient) : SalonApi {

    override suspend fun fetchHome(): NetworkResult<SalonHomeDto> = safeApiCall {
        httpClient.get(Endpoints.HOME)
    }

    override suspend fun fetchSalon(id: String): NetworkResult<SalonDto> = safeApiCall {
        httpClient.get("${Endpoints.SALON}/$id")
    }

    private object Endpoints {
        const val HOME = "/v1/api/salon/home"
        const val SALON = "/v1/api/salon"
    }
}
