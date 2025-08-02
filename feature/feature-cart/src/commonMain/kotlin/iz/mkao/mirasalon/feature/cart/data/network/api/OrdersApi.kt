package iz.mkao.mirasalon.feature.cart.data.network.api

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.CreateOrderRequest
import iz.mkao.mirasalon.core.network.model.dto.OrderDto

interface OrdersApi {
    suspend fun fetchMyOrders(
        status: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Outcome<PagedResponse<OrderDto>>
    
    suspend fun fetchOrderDetail(id: String): Outcome<OrderDto>

    suspend fun checkout(request: CreateOrderRequest): Outcome<OrderDto>

    suspend fun deleteOrder(id: String): Outcome<Unit>
}
