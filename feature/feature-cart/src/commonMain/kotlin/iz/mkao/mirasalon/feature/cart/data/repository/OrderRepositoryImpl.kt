package iz.mkao.mirasalon.feature.cart.data.repository

import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.database.dao.OrderDao
import iz.mkao.mirasalon.core.domain.model.Order
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.OrderRepository
import iz.mkao.mirasalon.core.network.model.dto.CreateOrderRequest
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.feature.cart.data.mapper.toDomain
import iz.mkao.mirasalon.feature.cart.data.mapper.toEntity
import iz.mkao.mirasalon.feature.cart.data.mapper.toRequest
import iz.mkao.mirasalon.feature.cart.data.network.api.OrdersApi
import iz.mkao.mirasalon.feature.profile.domain.model.UserProfile
import iz.mkao.mirasalon.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class OrderRepositoryImpl(
    private val api: OrdersApi,
    private val orderDao: OrderDao,
    private val profileRepository: ProfileRepository,
    private val realtimeGateway: RealtimeGateway,
    private val repositoryScope: CoroutineScope
) : OrderRepository {

    override fun observeOrders(): Flow<Outcome<List<Order>>> {
        return flow {
            val userId = (profileRepository.getProfile() as? Outcome.Success<UserProfile>)?.data?.id ?: ""
            emitAll(orderDao.getAllOrdersWithItems(userId))
        }
            .map { list -> Outcome.Success(list.map { it.toDomain() }) }
            .onStart {
                repositoryScope.launch { fetchOrders() }
            }
    }

    init {
        repositoryScope.launch {
            realtimeGateway.events.collectLatest { event ->
                when (event) {
                    is DomainEvent.OrderCreated,
                    is DomainEvent.OrderUpdated -> {
                        Napier.d("OrderRepository: Received order event, refreshing history...")
                        fetchOrders()
                    }
                    else -> {}
                }
            }
        }
    }

    override suspend fun fetchOrders(): Outcome<List<Order>> {
        val result = api.fetchMyOrders()
        
        return when (result) {
            is Outcome.Success -> {
                val profile = (profileRepository.getProfile() as? Outcome.Success<UserProfile>)?.data
                val dtos = result.data.items
                dtos.forEach { dto ->
                    val entity = dto.toEntity().copy(
                        userName = dto.userName ?: profile?.fullName ?: "Unknown",
                        userEmail = dto.userEmail ?: profile?.email ?: "unknown@example.com"
                    )
                    orderDao.saveOrderWithItems(
                        entity,
                        dto.items.map { it.toEntity(dto.id) }
                    )
                }
                Outcome.Success(dtos.map { it.toDomain() })
            }
            is Outcome.Error -> {
                Napier.e("OrderRepository: fetchOrders API error: ${result.failure}")
                Outcome.Error(result.failure)
            }
            is Outcome.Loading -> Outcome.Loading
        }
    }

    override suspend fun placeOrder(order: Order): Outcome<String> {
        // Industry Standard: Checkout is always Network-First.
        // We don't save to Room until the server confirms success.
        val request = CreateOrderRequest(
            items = order.items.map { it.toRequest() },
            promoCode = order.promoCode,
            subtotalAmount = order.subtotal,
            discountAmount = order.discount,
            shippingFees = order.shippingFees,
            totalAmount = order.total,
            salonId = "main-salon",
            shippingAddress = order.shippingAddress ?: "Mock Address",
            paymentMethod = order.paymentMethod ?: "Credit Card"
        )
        
        val result = api.checkout(request)
        
        if (result is Outcome.Success) {

            fetchOrders()
        }
        
        return result.map { it.id }
    }

    override suspend fun getOrderDetails(orderId: String): Outcome<Order> {

        val local = orderDao.getOrderById(orderId)
        if (local != null) {

            repositoryScope.launch {
                val network = api.fetchOrderDetail(orderId)
                if (network is Outcome.Success) {
                    val dto = network.data
                    val profile = (profileRepository.getProfile() as? Outcome.Success<UserProfile>)?.data
                    val entity = dto.toEntity().copy(
                        userName = dto.userName ?: profile?.fullName ?: "Unknown",
                        userEmail = dto.userEmail ?: profile?.email ?: "unknown@example.com"
                    )
                    orderDao.saveOrderWithItems(
                        entity,
                        dto.items.map { it.toEntity(dto.id) }
                    )
                }
            }
            return Outcome.Success(local.toDomain())
        }
        
        return api.fetchOrderDetail(orderId).map { it.toDomain() }
    }

    override suspend fun deleteOrder(orderId: String): Outcome<Unit> {
        return try {
            // First delete locally for instant UI update
            orderDao.deleteOrderById(orderId)
            
            // Then sync with server
            val result = api.deleteOrder(orderId)
            if (result is Outcome.Error) {
                // If server delete fails, we might want to re-fetch to restore state, 
                // but usually for a delete, we trust the UI action.
                Napier.e("OrderRepository: Failed to delete order $orderId on server: ${result.failure}")
            }
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Error(Failure.Unknown)
        }
    }
}
