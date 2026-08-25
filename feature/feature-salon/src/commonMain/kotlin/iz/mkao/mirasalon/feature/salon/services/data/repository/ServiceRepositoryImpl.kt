package iz.mkao.mirasalon.feature.salon.services.data.repository

import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.database.dao.ServiceCategoryDao
import iz.mkao.mirasalon.core.database.dao.ServiceDao
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.domain.model.ServiceFilter
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.ServiceRepository
import iz.mkao.mirasalon.core.network.mapper.toOutcome
import iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.feature.salon.services.data.mappers.toDomain
import iz.mkao.mirasalon.feature.salon.services.data.mappers.toEntity
import iz.mkao.mirasalon.feature.salon.services.data.network.api.ServicesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServiceRepositoryImpl(
    private val api: ServicesApi,
    private val serviceDao: ServiceDao,
    private val categoryDao: ServiceCategoryDao,
    private val repositoryScope: CoroutineScope
) : ServiceRepository {

    override fun observeCategories(): Flow<Outcome<List<ServiceCategory>>> {
        return categoryDao.getAllCategories()
            .map { entities -> 
                Outcome.Success(entities.map { it.toDomain() }) 
            }
            .onStart {
                repositoryScope.launch { fetchCategories() }
            }
    }

    override fun observeServices(filter: ServiceFilter): Flow<Outcome<List<Service>>> {
        val categoryId = filter.categoryId
        val baseFlow = if (categoryId != null) {
            serviceDao.getServicesByCategory(categoryId)
        } else {
            serviceDao.getAllServices()
        }

        return baseFlow
            .map { entities ->
                var services = entities.map { it.toDomain() }
                val query = filter.searchQuery
                if (!query.isNullOrBlank()) {
                    services = services.filter { 
                        it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
                    }
                }
                Outcome.Success(services)
            }
            .onStart {
                repositoryScope.launch { fetchServices(filter) }
            }
    }

    override suspend fun getCategories(): Outcome<List<ServiceCategory>> = withContext(Dispatchers.Default) {
        fetchCategories()
    }

    override suspend fun getServices(filter: ServiceFilter): Outcome<List<Service>> = withContext(Dispatchers.Default) {
        fetchServices(filter)
    }

    override suspend fun getService(serviceId: String): Outcome<Service> = withContext(Dispatchers.Default) {
        try {
            val response = api.fetchService(serviceId)
            response.toOutcome { it.toDomain() }
        } catch (e: Exception) {
            Outcome.Error(Failure.Unknown)
        }
    }

    override suspend fun submitReview(serviceId: String, rating: Int, comment: String, userId: String?): Outcome<Unit> = withContext(Dispatchers.Default) {
        Napier.d(tag = "ServiceRepositoryImpl") { "submitReview: serviceId=$serviceId, rating=$rating, userId=$userId" }
        try {
            val response = api.submitReview(
                serviceId, 
                SubmitReviewRequest(
                    rating = rating, 
                    comment = comment,
                    targetId = serviceId,
                    targetType = "SERVICE"
                )
            )
            val result = response.toOutcome { Unit }
            if (result is Outcome.Error) {
                Napier.e(tag = "ServiceRepositoryImpl") { "submitReview error: ${result.failure}" }
            }
            result
        } catch (e: Exception) {
            Napier.e(tag = "ServiceRepositoryImpl", throwable = e) { "submitReview exception" }
            Outcome.Error(Failure.Unknown)
        }
    }

    private suspend fun fetchCategories(): Outcome<List<ServiceCategory>> {
        return try {
            val response = api.fetchCategories()
            when (response) {
                is NetworkResult.Success -> {
                    val dtos = response.data
                    categoryDao.upsertCategories(dtos.map { it.toEntity() })
                    Outcome.Success(dtos.map { it.toDomain() })
                }
                is NetworkResult.Error -> Outcome.Error(Failure.NetworkConnection(response.error.message))
            }
        } catch (e: Exception) {
            Outcome.Error(Failure.Unknown)
        }
    }

    private suspend fun fetchServices(filter: ServiceFilter): Outcome<List<Service>> {
        return try {
            when (val response = api.fetchServices(filter.categoryId, filter.searchQuery)) {
                is NetworkResult.Success -> {
                    val dtos = response.data
                    serviceDao.upsertServices(dtos.map { it.toEntity() })
                    Outcome.Success(dtos.map { it.toDomain() })
                }
                is NetworkResult.Error -> Outcome.Error(Failure.NetworkConnection(response.error.message))
            }
        } catch (e: Exception) {
            Outcome.Error(Failure.Unknown)
        }
    }
}
