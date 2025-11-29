package iz.mkao.mirasalon.data.repository

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.AdminServiceRepository
import iz.mkao.mirasalon.core.network.client.admin.AdminServicesApi
import iz.mkao.mirasalon.core.network.mapper.admin.toDomain
import iz.mkao.mirasalon.core.network.model.dto.CreateServiceCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.CreateServiceRequestDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateServiceCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateServiceRequestDto

class KtorAdminServiceRepository(
    private val api: AdminServicesApi
) : AdminServiceRepository {

    override suspend fun getCategories(): Outcome<List<ServiceCategory>> {
        return api.fetchCategories().map { dtos ->
            dtos.map { it.toDomain() }
        }
    }

    override suspend fun getServices(categoryId: String?, query: String?): Outcome<List<Service>> {
        return api.fetchServices(categoryId, query).map { dtos ->
            dtos.map { it.toDomain() }
        }
    }

    override suspend fun create(
        name: String,
        categoryId: String,
        subCategory: String?,
        price: Double,
        durationMinutes: Int,
        description: String,
        imageUrl: String?
    ): Outcome<Unit> {
        val request = CreateServiceRequestDto(
            name = name,
            categoryId = categoryId,
            subCategory = subCategory,
            price = price,
            durationMinutes = durationMinutes,
            description = description,
            imageUrl = imageUrl
        )
        return api.createService(request).map { Unit }
    }

    override suspend fun update(
        id: String,
        name: String?,
        categoryId: String?,
        subCategory: String?,
        price: Double?,
        durationMinutes: Int?,
        description: String?,
        imageUrl: String?
    ): Outcome<Unit> {
        val request = UpdateServiceRequestDto(
            name = name,
            categoryId = categoryId,
            subCategory = subCategory,
            price = price,
            durationMinutes = durationMinutes,
            description = description,
            imageUrl = imageUrl
        )
        return api.updateService(id, request)
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        return api.deleteService(id)
    }

    override suspend fun createCategory(name: String, iconName: String?, imageUrl: String?): Outcome<Unit> {
        val request = CreateServiceCategoryRequest(name, iconName, imageUrl)
        return api.createCategory(request).map { Unit }
    }

    override suspend fun updateCategory(id: String, name: String?, iconName: String?, imageUrl: String?): Outcome<Unit> {
        val request = UpdateServiceCategoryRequest(name, iconName, imageUrl)
        return api.updateCategory(id, request)
    }

    override suspend fun deleteCategory(id: String): Outcome<Unit> {
        return api.deleteCategory(id)
    }
}
