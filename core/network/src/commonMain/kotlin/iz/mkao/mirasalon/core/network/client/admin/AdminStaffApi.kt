package iz.mkao.mirasalon.core.network.client.admin

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.*

interface AdminStaffApi {
    suspend fun fetchStaff(query: String? = null): Outcome<List<SpecialistDto>>
    suspend fun createStaff(request: CreateSpecialistRequestDto): Outcome<SpecialistDto>
    suspend fun updateStaff(id: String, request: UpdateSpecialistRequestDto): Outcome<Unit>
    suspend fun updateStaffStatus(id: String, status: String): Outcome<Unit>
    suspend fun updateStaffActive(id: String, isActive: Boolean): Outcome<Unit>
    suspend fun fetchStaffStats(id: String): Outcome<SpecialistPerformanceDto>
    suspend fun updateStaffShifts(id: String, shifts: List<SpecialistShiftDto>): Outcome<Unit>
    suspend fun deleteStaff(id: String): Outcome<Unit>
}
