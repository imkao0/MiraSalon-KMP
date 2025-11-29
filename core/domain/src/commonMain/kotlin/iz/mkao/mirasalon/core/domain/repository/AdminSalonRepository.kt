package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.AdminSalon
import iz.mkao.mirasalon.core.domain.outcome.Outcome

interface AdminSalonRepository {
    suspend fun getManagementInfo(): Outcome<List<AdminSalon>>
    suspend fun updateSalon(id: String, salon: AdminSalon): Outcome<Unit>
}
