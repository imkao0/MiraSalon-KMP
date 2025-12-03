package iz.mkao.mirasalon.core.network.client.admin

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.CreateServiceCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.CreateServiceRequestDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceCategoryDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateServiceCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateServiceRequestDto

interface AdminServicesApi {
    suspend fun fetchCategories(): Outcome<List<ServiceCategoryDto>>
    suspend fun fetchServices(categoryId: String?, query: String? = null): Outcome<List<ServiceDto>>
    suspend fun createService(request: CreateServiceRequestDto): Outcome<ServiceDto>
    suspend fun updateService(id: String, request: UpdateServiceRequestDto): Outcome<Unit>
    suspend fun deleteService(id: String): Outcome<Unit>

    suspend fun createCategory(request: CreateServiceCategoryRequest): Outcome<ServiceCategoryDto>
    suspend fun updateCategory(id: String, request: UpdateServiceCategoryRequest): Outcome<Unit>
    suspend fun deleteCategory(id: String): Outcome<Unit>
}
