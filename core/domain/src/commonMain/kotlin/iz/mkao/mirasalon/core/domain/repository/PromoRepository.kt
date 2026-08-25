package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.PromoValidation
import iz.mkao.mirasalon.core.domain.model.CartItem
import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import kotlinx.coroutines.flow.Flow

interface PromoRepository {
    suspend fun validatePromo(code: String, cartItems: List<CartItem>): Outcome<PromoValidation>
    fun observePromotions(): Flow<Outcome<List<Promotion>>>
    suspend fun fetchPromotions(): Outcome<List<Promotion>>
    suspend fun getUsedPromotionIds(): Outcome<List<String>>
}
