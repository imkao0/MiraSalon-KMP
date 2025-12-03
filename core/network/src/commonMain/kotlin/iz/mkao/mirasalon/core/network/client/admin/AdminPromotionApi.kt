package iz.mkao.mirasalon.core.network.client.admin

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.CreatePromotionRequestDto
import iz.mkao.mirasalon.core.network.model.dto.PromotionDto
import iz.mkao.mirasalon.core.network.model.dto.UpdatePromotionRequestDto

interface AdminPromotionApi {
    suspend fun fetchAllPromotions(
        query: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Outcome<PagedResponse<PromotionDto>>
    suspend fun createPromotion(request: CreatePromotionRequestDto): Outcome<PromotionDto>
    suspend fun updatePromotion(id: String, request: UpdatePromotionRequestDto): Outcome<Unit>
    suspend fun deletePromotion(id: String): Outcome<Unit>
    suspend fun deleteAllPromotions(): Outcome<Unit>
}
