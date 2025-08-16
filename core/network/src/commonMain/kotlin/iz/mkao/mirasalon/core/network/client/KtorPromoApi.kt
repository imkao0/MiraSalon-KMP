package iz.mkao.mirasalon.core.network.client

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.domain.model.PromoValidation
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.PromotionDto
import iz.mkao.mirasalon.core.network.model.dto.ValidatePromoRequest
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.network.result.apiCall
import iz.mkao.mirasalon.core.network.result.safeApiCall

class KtorPromoApi(private val httpClient: HttpClient) : PromoApi {
    override suspend fun validatePromo(request: ValidatePromoRequest): Outcome<PromoValidation> = apiCall {
        httpClient.post(Endpoints.VALIDATE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun fetchActivePromotions(): NetworkResult<List<PromotionDto>> = safeApiCall {
        httpClient.get(Endpoints.ACTIVE)
    }

    override suspend fun fetchUsedPromotionIds(): Outcome<List<String>> = apiCall {
        httpClient.get(Endpoints.USED)
    }

    private object Endpoints {
        const val VALIDATE = "/v1/api/promotions/validate"
        const val ACTIVE = "/v1/api/promotions/active"
        const val USED = "/v1/api/promotions/used"
    }
}
