package iz.mkao.mirasalon.core.network.client.admin

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.SalonPaginatedResponseDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateSalonRequest

interface AdminSalonApi {
    suspend fun fetchManagementInfo(page: Int = 1, pageSize: Int = 20): Outcome<SalonPaginatedResponseDto>
    suspend fun updateSalon(id: String, request: UpdateSalonRequest): Outcome<Unit>
}
