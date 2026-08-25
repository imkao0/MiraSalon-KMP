package iz.mkao.mirasalon.feature.specialists.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.SpecialistDto
import iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorSpecialistsApi(private val httpClient: HttpClient) : SpecialistsApi {

    override suspend fun fetchSpecialists(): Outcome<List<SpecialistDto>> = apiCall<List<SpecialistDto>> {
        httpClient.get(Endpoints.SPECIALISTS)
    }

    override suspend fun fetchSpecialist(id: String): Outcome<SpecialistDto> = apiCall<SpecialistDto> {
        httpClient.get(Endpoints.specialist(id))
    }

    override suspend fun submitReview(specialistId: String, rating: Int, comment: String): Outcome<Unit> = apiCall<String> {
        httpClient.post(Endpoints.reviews(specialistId)) {
            contentType(ContentType.Application.Json)
            setBody(SubmitReviewRequest(rating, comment, targetId = specialistId, targetType = "SPECIALIST"))
        }
    }.map { Unit }

    private object Endpoints {
        const val SPECIALISTS = "/v1/api/specialists"
        fun specialist(id: String) = "/v1/api/specialists/$id"
        fun reviews(id: String) = "/v1/api/specialists/$id/reviews"
    }
}
