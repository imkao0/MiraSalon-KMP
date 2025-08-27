package iz.mkao.mirasalon.feature.salon.services.data.repository

import iz.mkao.mirasalon.core.database.datasource.PromotionLocalDataSource
import iz.mkao.mirasalon.core.domain.model.CartItem
import iz.mkao.mirasalon.core.domain.model.PromoValidation
import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.PromoRepository
import iz.mkao.mirasalon.core.network.client.PromoApi
import iz.mkao.mirasalon.core.network.mapper.admin.toClientDomain
import iz.mkao.mirasalon.core.network.model.dto.ValidatePromoRequest
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PromotionRepositoryImpl(
    private val promoApi: PromoApi,
    private val localDataSource: PromotionLocalDataSource,
    private val realtimeGateway: RealtimeGateway,
    private val repositoryScope: CoroutineScope
) : PromoRepository {

    init {
        observeRealtimeEvents()
    }

    private fun observeRealtimeEvents() {
        repositoryScope.launch {
            realtimeGateway.events.collectLatest { event ->
                if (event is DomainEvent.PromotionChanged) {
                    fetchPromotions()
                }
            }
        }
    }

    override suspend fun validatePromo(code: String, cartItems: List<CartItem>): Outcome<PromoValidation> {
        val cartTotal = cartItems.sumOf { it.product.discountedPrice * it.quantity }
        val serviceIds = cartItems.map { it.product.id }
        
        return promoApi.validatePromo(
            ValidatePromoRequest(
                code = code,
                cartTotal = cartTotal,
                serviceIds = serviceIds
            )
        )
    }

    override fun observePromotions(): Flow<Outcome<List<Promotion>>> {
        return localDataSource.observeActivePromotions()
            .map { Outcome.Success(it) }
            .onStart {
                repositoryScope.launch { fetchPromotions() }
            }
    }

    override suspend fun fetchPromotions(): Outcome<List<Promotion>> = withContext(Dispatchers.Default) {
        try {
            val response = promoApi.fetchActivePromotions()
            when (response) {
                is NetworkResult.Success -> {
                    val promotions = response.data.map { it.toClientDomain() }
                    Outcome.Success(promotions)
                }
                is NetworkResult.Error -> {
                    Outcome.Error(Failure.NetworkConnection(response.error.message))
                }
            }
        } catch (e: Exception) {
            Outcome.Error(Failure.Unknown)
        }
    }

    override suspend fun getUsedPromotionIds(): Outcome<List<String>> = withContext(Dispatchers.Default) {
        try {
            promoApi.fetchUsedPromotionIds()
        } catch (e: Exception) {
            Outcome.Error(Failure.Unknown)
        }
    }
}
