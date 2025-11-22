package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.AdminReview
import iz.mkao.mirasalon.core.domain.outcome.Outcome

interface ReviewsRepository {
    suspend fun getAll(query: String? = null): Outcome<List<AdminReview>>
    suspend fun updateVisibility(id: String, isVisible: Boolean): Outcome<Unit>
    suspend fun delete(id: String): Outcome<Unit>
    suspend fun postReply(id: String, reply: String): Outcome<Unit>
}
