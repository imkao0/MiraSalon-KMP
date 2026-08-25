package iz.mkao.mirasalon.feature.salon.services.data.network.api

import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.network.model.dto.ServiceCategoryDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest

interface ServicesApi {
    suspend fun fetchCategories(): NetworkResult<List<ServiceCategoryDto>>
    suspend fun fetchServices(categoryId: String?, query: String?): NetworkResult<List<ServiceDto>>
    suspend fun fetchService(id: String): NetworkResult<ServiceDto>
    suspend fun submitReview(serviceId: String, request: SubmitReviewRequest): NetworkResult<String>
}
