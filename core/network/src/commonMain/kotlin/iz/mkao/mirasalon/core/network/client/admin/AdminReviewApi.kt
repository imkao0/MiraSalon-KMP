package iz.mkao.mirasalon.core.network.client.admin

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.AdminReviewDto

interface AdminReviewApi {
    suspend fun fetchAllReviews(
        query: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Outcome<PagedResponse<AdminReviewDto>>
    suspend fun postAdminReply(reviewId: String, reply: String): Outcome<Unit>
    suspend fun updateVisibility(reviewId: String, isVisible: Boolean): Outcome<Unit>
    suspend fun deleteReview(reviewId: String): Outcome<Unit>
}
