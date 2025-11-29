package iz.mkao.mirasalon.data.repository

import iz.mkao.mirasalon.core.domain.model.AdminSalon
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.AdminSalonRepository
import iz.mkao.mirasalon.core.network.client.admin.AdminSalonApi
import iz.mkao.mirasalon.core.network.mapper.admin.toDomain
import iz.mkao.mirasalon.core.network.mapper.admin.toUpdateRequest

class KtorAdminSalonRepository(
    private val api: AdminSalonApi
) : AdminSalonRepository {

    override suspend fun getManagementInfo(): Outcome<List<AdminSalon>> {
        return api.fetchManagementInfo().map { paged ->
            paged.items.map { it.toDomain() }
        }
    }

    override suspend fun updateSalon(id: String, salon: AdminSalon): Outcome<Unit> {
        return api.updateSalon(id, salon.toUpdateRequest())
    }
}
