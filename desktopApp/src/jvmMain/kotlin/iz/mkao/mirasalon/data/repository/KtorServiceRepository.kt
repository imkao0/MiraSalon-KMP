package iz.mkao.mirasalon.data.repository

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.domain.model.ServiceFilter
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.ServiceRepository
import iz.mkao.mirasalon.core.network.client.admin.AdminServicesApi
import iz.mkao.mirasalon.core.network.mapper.admin.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorServiceRepository(
    private val api: AdminServicesApi
) : ServiceRepository {

    override suspend fun getCategories(): Outcome<List<ServiceCategory>> {
        return api.fetchCategories().map { dtos ->
            dtos.map { it.toDomain() }
        }
    }

    override suspend fun getServices(filter: ServiceFilter): Outcome<List<Service>> {
        return api.fetchServices(filter.categoryId, filter.searchQuery).map { dtos ->
            dtos.map { it.toDomain() }
        }
    }

    override fun observeCategories(): Flow<Outcome<List<ServiceCategory>>> = flow {
        emit(Outcome.Loading)
        emit(getCategories())
    }

    override fun observeServices(filter: ServiceFilter): Flow<Outcome<List<Service>>> = flow {
        emit(Outcome.Loading)
        emit(getServices(filter))
    }

    override suspend fun getService(serviceId: String): Outcome<Service> {
        return api.fetchServices(null).map { dtos ->
            dtos.find { it.id == serviceId }?.toDomain() 
                ?: throw Exception("Service not found")
        }
    }

    override suspend fun submitReview(serviceId: String, rating: Int, comment: String, userId: String?): Outcome<Unit> {
        return Outcome.Error(Failure.Unknown)
    }
}
