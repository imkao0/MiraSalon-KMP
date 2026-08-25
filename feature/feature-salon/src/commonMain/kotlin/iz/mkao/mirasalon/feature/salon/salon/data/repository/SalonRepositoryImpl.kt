package iz.mkao.mirasalon.feature.salon.salon.data.repository

import iz.mkao.mirasalon.core.domain.model.Salon
import iz.mkao.mirasalon.core.domain.model.SalonHome
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.repository.SalonRepository
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.mapper.toOutcome
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.feature.salon.salon.data.mapper.toDomain
import iz.mkao.mirasalon.feature.salon.salon.data.network.api.SalonApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

class SalonRepositoryImpl(
    private val api: SalonApi,
    private val realtimeGateway: RealtimeGateway,
    private val repositoryScope: CoroutineScope
) : SalonRepository {

    private val _homeCache = MutableStateFlow<Outcome<SalonHome>>(Outcome.Loading)
    private val _salonCache = mutableMapOf<String, Salon>()

    init {
        repositoryScope.launch {
            realtimeGateway.events.collectLatest { event ->
                when (event) {
                    is DomainEvent.PromotionChanged,
                    is DomainEvent.ServiceUpdated -> {

                    }
                    else -> {}
                }
            }
        }
    }

    override suspend fun getHome(): Outcome<SalonHome> {
        val result = api.fetchHome().toOutcome { it.toDomain() }
        if (result is Outcome.Success) {
            _homeCache.value = result
        }
        return result
    }

    override suspend fun getSalon(id: String): Outcome<Salon> {
        _salonCache[id]?.let { return Outcome.Success(it) }
        val result = api.fetchSalon(id).toOutcome { it.toDomain() }
        if (result is Outcome.Success) {
            _salonCache[id] = result.data
        }
        return result
    }
}
