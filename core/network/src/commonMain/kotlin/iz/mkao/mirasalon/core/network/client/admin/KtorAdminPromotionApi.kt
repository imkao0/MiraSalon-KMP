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
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.CreatePromotionRequestDto
import iz.mkao.mirasalon.core.network.model.dto.PromotionDto
import iz.mkao.mirasalon.core.network.model.dto.UpdatePromotionRequestDto
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorAdminPromotionApi(private val httpClient: HttpClient) : AdminPromotionApi {

    override suspend fun fetchAllPromotions(
        query: String?,
        page: Int?,
        pageSize: Int?
    ): Outcome<PagedResponse<PromotionDto>> = apiCall<PagedResponse<PromotionDto>> {
        httpClient.get(Endpoints.PROMOTIONS_ALL) {
            parameter("query", query)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }
    }

    override suspend fun createPromotion(request: CreatePromotionRequestDto): Outcome<PromotionDto> = apiCall<PromotionDto> {
        httpClient.post(Endpoints.PROMOTIONS) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun updatePromotion(id: String, request: UpdatePromotionRequestDto): Outcome<Unit> = apiCall<Unit> {
        httpClient.put(Endpoints.promotion(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun deletePromotion(id: String): Outcome<Unit> = apiCall<Unit> {
        httpClient.delete(Endpoints.promotion(id))
    }

    override suspend fun deleteAllPromotions(): Outcome<Unit> = apiCall<Unit> {
        httpClient.delete(Endpoints.PROMOTIONS_ALL)
    }

    private object Endpoints {
        const val PROMOTIONS = "/v1/api/promotions"
        const val PROMOTIONS_ALL = "/v1/api/promotions/all"
        fun promotion(id: String) = "/v1/api/promotions/$id"
    }
}
