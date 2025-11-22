package iz.mkao.mirasalon.data.repository

import iz.mkao.mirasalon.core.domain.model.AdminReview
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.ReviewsRepository
import iz.mkao.mirasalon.core.network.client.admin.AdminReviewApi
import iz.mkao.mirasalon.core.network.mapper.admin.toDomain

class KtorAdminReviewRepository(
    private val api: AdminReviewApi
) : ReviewsRepository {

    override suspend fun getAll(query: String?): Outcome<List<AdminReview>> {
        return api.fetchAllReviews(query = query).map { pagedResponse ->
            pagedResponse.items.map { it.toDomain() }
        }
    }

    override suspend fun updateVisibility(id: String, isVisible: Boolean): Outcome<Unit> {
        return api.updateVisibility(id, isVisible)
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        return api.deleteReview(id)
    }

    override suspend fun postReply(id: String, reply: String): Outcome<Unit> {
        return api.postAdminReply(id, reply)
    }
}
