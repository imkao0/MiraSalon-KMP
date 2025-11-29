package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.domain.outcome.Outcome

interface AdminServiceRepository {
    suspend fun getCategories(): Outcome<List<ServiceCategory>>
    suspend fun getServices(categoryId: String? = null, query: String? = null): Outcome<List<Service>>
    suspend fun create(name: String, categoryId: String, subCategory: String?, price: Double, durationMinutes: Int, description: String, imageUrl: String?): Outcome<Unit>
    suspend fun update(id: String, name: String?, categoryId: String?, subCategory: String?, price: Double?, durationMinutes: Int?, description: String?, imageUrl: String?): Outcome<Unit>
    suspend fun delete(id: String): Outcome<Unit>

    // Category Management
    suspend fun createCategory(name: String, iconName: String?, imageUrl: String?): Outcome<Unit>
    suspend fun updateCategory(id: String, name: String?, iconName: String?, imageUrl: String?): Outcome<Unit>
    suspend fun deleteCategory(id: String): Outcome<Unit>
}
