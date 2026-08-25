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
import iz.mkao.mirasalon.core.network.model.dto.AdminReplyRequest
import iz.mkao.mirasalon.core.network.model.dto.AdminReviewDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateReviewVisibilityRequest
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorAdminReviewApi(private val httpClient: HttpClient) : AdminReviewApi {

    override suspend fun fetchAllReviews(
        query: String?,
        page: Int?,
        pageSize: Int?
    ): Outcome<PagedResponse<AdminReviewDto>> = apiCall<PagedResponse<AdminReviewDto>> {
        httpClient.get(Endpoints.ALL_REVIEWS) {
            parameter("query", query)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }
    }

    override suspend fun postAdminReply(reviewId: String, reply: String): Outcome<Unit> = apiCall<Unit> {
        httpClient.post(Endpoints.reply(reviewId)) {
            contentType(ContentType.Application.Json)
            setBody(AdminReplyRequest(reply))
        }
    }

    override suspend fun updateVisibility(reviewId: String, isVisible: Boolean): Outcome<Unit> = apiCall<Unit> {
        httpClient.put(Endpoints.visibility(reviewId)) {
            contentType(ContentType.Application.Json)
            setBody(UpdateReviewVisibilityRequest(isVisible))
        }
    }

    override suspend fun deleteReview(reviewId: String): Outcome<Unit> = apiCall<Unit> {
        httpClient.delete(Endpoints.review(reviewId))
    }

    private object Endpoints {
        const val ALL_REVIEWS = "/v1/api/reviews/all"
        fun reply(id: String) = "/v1/api/reviews/$id/reply"
        fun visibility(id: String) = "/v1/api/reviews/$id/visibility"
        fun review(id: String) = "/v1/api/reviews/$id"
    }
}
