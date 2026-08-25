package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.domain.model.ServiceFilter
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import kotlinx.coroutines.flow.Flow

interface ServiceRepository {
    fun observeCategories(): Flow<Outcome<List<ServiceCategory>>>
    fun observeServices(filter: ServiceFilter): Flow<Outcome<List<Service>>>
    
    suspend fun getCategories(): Outcome<List<ServiceCategory>>
    suspend fun getServices(filter: ServiceFilter): Outcome<List<Service>>
    suspend fun getService(serviceId: String): Outcome<Service>
    
    suspend fun submitReview(serviceId: String, rating: Int, comment: String, userId: String? = null): Outcome<Unit>
}
