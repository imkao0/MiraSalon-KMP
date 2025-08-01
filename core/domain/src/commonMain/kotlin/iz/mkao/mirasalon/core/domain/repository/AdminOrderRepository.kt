package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.AdminOrder
import iz.mkao.mirasalon.core.domain.model.AdminOrderStatus
import iz.mkao.mirasalon.core.domain.outcome.Outcome

interface AdminOrderRepository {
    suspend fun getAll(
        status: AdminOrderStatus? = null,
        query: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null
    ): Outcome<List<AdminOrder>>
    suspend fun updateStatus(id: String, status: AdminOrderStatus): Outcome<Unit>
    suspend fun delete(id: String): Outcome<Unit>
}
