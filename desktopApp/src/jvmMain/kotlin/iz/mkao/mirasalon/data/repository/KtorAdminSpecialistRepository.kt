package iz.mkao.mirasalon.data.repository

import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.domain.model.AdminSpecialist
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistShift
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistStats
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.AdminSpecialistRepository
import iz.mkao.mirasalon.core.network.client.admin.AdminStaffApi
import iz.mkao.mirasalon.core.network.mapper.admin.toAdminSpecialist
import iz.mkao.mirasalon.core.network.mapper.admin.toDomain
import iz.mkao.mirasalon.core.network.mapper.admin.toDto
import iz.mkao.mirasalon.core.network.model.dto.CreateSpecialistRequestDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateSpecialistRequestDto

class KtorAdminSpecialistRepository(
    private val api: AdminStaffApi
) : AdminSpecialistRepository {

    override suspend fun getAll(query: String?): Outcome<List<AdminSpecialist>> {
        return api.fetchStaff(query).map { dtos ->
            dtos.map { it.toAdminSpecialist() }
        }
    }

    override suspend fun create(specialist: AdminSpecialist): Outcome<Unit> {
        val request = CreateSpecialistRequestDto(
            name = specialist.name,
            role = specialist.role,
            salonId = specialist.salonId.ifBlank { "main-salon" },
            imageUrl = specialist.imageUrl,
            bio = specialist.bio,
            yearsOfExperience = specialist.yearsOfExperience,
            serviceIds = specialist.services.map { it.id }
        )
        return api.createStaff(request).map { Unit }
    }

    override suspend fun update(specialist: AdminSpecialist): Outcome<Unit> {
        val request = UpdateSpecialistRequestDto(
            name = specialist.name,
            role = specialist.role,
            imageUrl = specialist.imageUrl,
            bio = specialist.bio,
            yearsOfExperience = specialist.yearsOfExperience,
            serviceIds = specialist.services.map { it.id }
        )
        
        Napier.d { "[KtorAdminSpecialistRepository] Updating specialist ${specialist.id}, imageUrl: ${request.imageUrl}" }

        return api.updateStaff(specialist.id, request)
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        return api.deleteStaff(id)
    }

    override suspend fun getStats(id: String): Outcome<AdminSpecialistStats> {
        return api.fetchStaffStats(id).map { it.toDomain() }
    }

    override suspend fun updateShifts(specialistId: String, shifts: List<AdminSpecialistShift>): Outcome<Unit> {
        return api.updateStaffShifts(specialistId, shifts.map { it.toDto() })
    }

    override suspend fun updateAvailability(id: String, isAvailable: Boolean): Outcome<Unit> {
        return api.updateStaffStatus(id, if (isAvailable) "ONLINE" else "AWAY")
    }

    override suspend fun updateActiveStatus(id: String, isActive: Boolean): Outcome<Unit> {
        return api.updateStaffActive(id, isActive)
    }
}
