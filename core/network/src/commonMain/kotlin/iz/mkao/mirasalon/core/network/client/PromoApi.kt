package iz.mkao.mirasalon.core.network.client

import iz.mkao.mirasalon.core.domain.model.PromoValidation
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.PromotionDto
import iz.mkao.mirasalon.core.network.model.dto.ValidatePromoRequest
import iz.mkao.mirasalon.core.network.result.NetworkResult

interface PromoApi {
    suspend fun validatePromo(request: ValidatePromoRequest): Outcome<PromoValidation>
    suspend fun fetchActivePromotions(): NetworkResult<List<PromotionDto>>
    suspend fun fetchUsedPromotionIds(): Outcome<List<String>>
}
