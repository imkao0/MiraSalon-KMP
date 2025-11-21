package iz.mkao.mirasalon.data.repository

import iz.mkao.mirasalon.core.domain.model.AdminPromotion
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.AdminPromotionRepository
import iz.mkao.mirasalon.core.network.client.admin.AdminPromotionApi
import iz.mkao.mirasalon.core.network.mapper.admin.toCreateDto
import iz.mkao.mirasalon.core.network.mapper.admin.toDomain
import iz.mkao.mirasalon.core.network.mapper.admin.toUpdateDto

class KtorAdminPromotionRepository(
    private val api: AdminPromotionApi
) : AdminPromotionRepository {

    override suspend fun getAll(query: String?): Outcome<List<AdminPromotion>> {
        return api.fetchAllPromotions(query = query).map { pagedResponse ->
            pagedResponse.items.map { it.toDomain() }
        }
    }

    override suspend fun create(promotion: AdminPromotion): Outcome<Unit> {
        return api.createPromotion(promotion.toCreateDto()).map { Unit }
    }

    override suspend fun update(promotion: AdminPromotion): Outcome<Unit> {
        return api.updatePromotion(promotion.id, promotion.toUpdateDto())
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        return api.deletePromotion(id)
    }

    override suspend fun clearAll(): Outcome<Unit> {
        return api.deleteAllPromotions()
    }
}
