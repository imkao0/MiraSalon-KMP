package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.AdminPromotion
import iz.mkao.mirasalon.core.domain.outcome.Outcome

interface AdminPromotionRepository {
    suspend fun getAll(query: String? = null): Outcome<List<AdminPromotion>>
    suspend fun create(promotion: AdminPromotion): Outcome<Unit>
    suspend fun update(promotion: AdminPromotion): Outcome<Unit>
    suspend fun delete(id: String): Outcome<Unit>
    suspend fun clearAll(): Outcome<Unit>
}
