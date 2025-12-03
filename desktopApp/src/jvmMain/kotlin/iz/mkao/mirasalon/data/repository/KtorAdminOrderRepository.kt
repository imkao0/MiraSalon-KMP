package iz.mkao.mirasalon.data.repository

import iz.mkao.mirasalon.core.domain.model.AdminOrder
import iz.mkao.mirasalon.core.domain.model.AdminOrderStatus
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.AdminOrderRepository
import iz.mkao.mirasalon.core.network.client.admin.AdminOrdersApi
import iz.mkao.mirasalon.core.network.mapper.admin.toDomain

class KtorAdminOrderRepository(
    private val api: AdminOrdersApi
) : AdminOrderRepository {

    override suspend fun getAll(
        status: AdminOrderStatus?,
        query: String?,
        dateFrom: Long?,
        dateTo: Long?
    ): Outcome<List<AdminOrder>> {
        return api.fetchOrders(
            status = status?.name,
            query = query,
            dateFrom = dateFrom,
            dateTo = dateTo
        ).map { response ->
            response.items.map { it.toDomain() }
        }
    }

    override suspend fun updateStatus(id: String, status: AdminOrderStatus): Outcome<Unit> {
        return api.updateOrderStatus(id, status.name)
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        return api.deleteOrder(id)
    }
}
