package iz.mkao.mirasalon.feature.specialists.data.repository

import iz.mkao.mirasalon.core.database.dao.ServiceDao
import iz.mkao.mirasalon.core.database.dao.SpecialistDao
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.feature.specialists.data.mapper.toDomain
import iz.mkao.mirasalon.feature.specialists.data.mapper.toEntity
import iz.mkao.mirasalon.feature.specialists.data.mapper.toServiceEntities
import iz.mkao.mirasalon.feature.specialists.data.mapper.toSpecialistServiceRelations
import iz.mkao.mirasalon.feature.specialists.data.network.api.SpecialistsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class SpecialistRepositoryImpl(
    private val api: SpecialistsApi,
    private val specialistDao: SpecialistDao,
    private val serviceDao: ServiceDao,
    private val realtimeGateway: RealtimeGateway,
    private val repositoryScope: CoroutineScope,
) : SpecialistRepository {

    init {
        observeRealtimeEvents()
    }

    private fun observeRealtimeEvents() {
        realtimeGateway.events
            .onEach { event ->
                if (event is DomainEvent.SpecialistStatusChanged) {
                    if (event.specialistId.isEmpty()) {
                        refresh()
                    } else {
                        refreshSingleSpecialist(event.specialistId)
                    }
                }
                if (event is DomainEvent.ReviewSubmitted) {
                    if (event.targetType == "SPECIALIST") {
                        refreshSingleSpecialist(event.targetId)
                    } else if (event.targetType == "APPOINTMENT" || event.targetType == "SERVICE") {
                        // Appointment reviews often impact specialists, and service reviews might too.
                        // Since we don't know which specialist, we refresh all or refresh current ones.
                        refresh()
                    }
                }
            }
            .launchIn(repositoryScope)
    }

    override suspend fun refresh() {
        getSpecialists()
    }

    private suspend fun refreshSingleSpecialist(id: String) {
        getSpecialist(id)
    }

    override fun observeSpecialists(): Flow<Outcome<List<Specialist>>> =
        specialistDao.getAllSpecialistsWithServices()
            .map { list -> Outcome.Success(list.map { it.toDomain() }) }
            .onStart { repositoryScope.launch { refresh() } }

    override fun observeSpecialist(id: String): Flow<Outcome<Specialist>> =
        specialistDao.observeSpecialistById(id)
            .map { relation ->
                if (relation != null) Outcome.Success(relation.toDomain())
                else Outcome.Error(Failure.Unknown)
            }
            .onStart { repositoryScope.launch { refreshSingleSpecialist(id) } }

    override suspend fun getSpecialists(): Outcome<List<Specialist>> {
        val networkResult = api.fetchSpecialists()
        if (networkResult is Outcome.Success) {
            val dtos = networkResult.data
            specialistDao.upsertSpecialists(dtos.map { it.toEntity() })
            dtos.forEach { dto ->
                serviceDao.upsertServices(dto.toServiceEntities())
                specialistDao.deleteSpecialistServices(dto.id)
                specialistDao.upsertSpecialistServices(dto.toSpecialistServiceRelations())
            }
            return Outcome.Success(dtos.map { it.toDomain() })
        }
        return when (networkResult) {
            is Outcome.Error -> Outcome.Error(networkResult.failure)
            is Outcome.Loading -> Outcome.Loading
        }
    }

    override suspend fun getSpecialist(id: String): Outcome<Specialist> {
        val networkResult = api.fetchSpecialist(id)
        if (networkResult is Outcome.Success) {
            val dto = networkResult.data
            specialistDao.upsertSpecialists(listOf(dto.toEntity()))
            serviceDao.upsertServices(dto.toServiceEntities())
            specialistDao.deleteSpecialistServices(dto.id)
            specialistDao.upsertSpecialistServices(dto.toSpecialistServiceRelations())
            return Outcome.Success(dto.toDomain())
        }
        return when (networkResult) {
            is Outcome.Error -> Outcome.Error(networkResult.failure)
            is Outcome.Loading -> Outcome.Loading
        }
    }

    override suspend fun submitReview(
        specialistId: String,
        rating: Int,
        comment: String
    ): Outcome<Unit> {
        val result = api.submitReview(specialistId, rating, comment)
        if (result is Outcome.Success) {
            refreshSingleSpecialist(specialistId)
        }
        return result
    }
}
