package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.AdminSpecialist
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistShift
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistStats
import iz.mkao.mirasalon.core.domain.outcome.Outcome

interface AdminSpecialistRepository {
    suspend fun getAll(query: String? = null): Outcome<List<AdminSpecialist>>
    suspend fun create(specialist: AdminSpecialist): Outcome<Unit>
    suspend fun update(specialist: AdminSpecialist): Outcome<Unit>
    suspend fun delete(id: String): Outcome<Unit>
    suspend fun getStats(id: String): Outcome<AdminSpecialistStats>
    suspend fun updateShifts(specialistId: String, shifts: List<AdminSpecialistShift>): Outcome<Unit>
    suspend fun updateAvailability(id: String, isAvailable: Boolean): Outcome<Unit>
    suspend fun updateActiveStatus(id: String, isActive: Boolean): Outcome<Unit>
}
