package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.Order
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun observeOrders(): kotlinx.coroutines.flow.Flow<Outcome<List<Order>>>
    suspend fun fetchOrders(): Outcome<List<Order>>
    suspend fun placeOrder(order: Order): Outcome<String>
    suspend fun getOrderDetails(orderId: String): Outcome<Order>
    suspend fun deleteOrder(orderId: String): Outcome<Unit>
}
