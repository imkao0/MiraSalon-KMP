package iz.mkao.mirasalon.core.network.client.admin

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.OrderDto

interface AdminOrdersApi {
    suspend fun fetchOrders(
        status: String? = null,
        query: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Outcome<PagedResponse<OrderDto>>

    suspend fun updateOrderStatus(id: String, status: String): Outcome<Unit>
    suspend fun deleteOrder(id: String): Outcome<Unit>
}
