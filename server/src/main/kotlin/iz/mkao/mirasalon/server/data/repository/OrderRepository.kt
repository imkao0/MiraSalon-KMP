package iz.mkao.mirasalon.server.data.repository

import io.micrometer.core.instrument.MeterRegistry
import iz.mkao.mirasalon.core.domain.model.CartItem
import iz.mkao.mirasalon.core.domain.model.Order
import iz.mkao.mirasalon.core.domain.model.OrderStatus
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.OrderDto
import iz.mkao.mirasalon.core.network.model.dto.OrderItemDto
import iz.mkao.mirasalon.core.network.model.dto.OrderItemRequest
import iz.mkao.mirasalon.core.network.model.dto.OrderStatusDto
import iz.mkao.mirasalon.server.data.tables.OrderItemsTable
import iz.mkao.mirasalon.server.data.tables.OrdersTable
import iz.mkao.mirasalon.server.data.tables.ProductsTable
import iz.mkao.mirasalon.server.data.tables.PromotionsTable
import iz.mkao.mirasalon.server.data.tables.SalonsTable
import iz.mkao.mirasalon.server.data.tables.UsersTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.util.UUID
import iz.mkao.mirasalon.core.domain.repository.OrderRepository as CoreOrderRepository

enum class OrderRepoStatus {
    PENDING, SHIPPED, REFUNDED, DELIVERED, CANCELLED
}

sealed class OrderCreationResult {
    data class Success(val orderId: String) : OrderCreationResult()
    data class Error(val message: String) : OrderCreationResult()
}

sealed class OrderStatusUpdateResult {
    data object Success : OrderStatusUpdateResult()
    data object NotFound : OrderStatusUpdateResult()
    data object InvalidTransition : OrderStatusUpdateResult()
    data class DatabaseError(val cause: Exception) : OrderStatusUpdateResult()
}

class OrderRepository(
    private val outboxRepository: OutboxRepository,
    private val json: Json,
    private val meterRegistry: MeterRegistry,
    private val promotionRepository: PromotionRepository
) : CoreOrderRepository {

    private val log = LoggerFactory.getLogger(OrderRepository::class.java)

    fun countByStatusInRange(status: OrderRepoStatus, start: Long, end: Long): Int = transaction {
        OrdersTable.selectAll().where {
            (OrdersTable.status eq status.name) and
            (OrdersTable.createdAt greaterEq start) and
            (OrdersTable.createdAt less end)
        }.count().toInt()
    }

    fun totalRevenue(): Double = transaction {
        OrdersTable.selectAll().where {
            OrdersTable.status eq OrderRepoStatus.DELIVERED.name
        }.sumOf { it[OrdersTable.totalAmount] }
    }

    fun totalRevenueInRange(start: Long, end: Long): Double = transaction {
        OrdersTable.selectAll().where {
            (OrdersTable.status eq OrderRepoStatus.DELIVERED.name) and
            (OrdersTable.createdAt greaterEq start) and
            (OrdersTable.createdAt less end)
        }.sumOf { it[OrdersTable.totalAmount] }
    }

    fun findByDateRange(start: Long, end: Long): List<OrderDto> = transaction {
        (OrdersTable leftJoin UsersTable)
            .selectAll().where {
                (OrdersTable.createdAt greaterEq start) and
                (OrdersTable.createdAt less end)
            }
            .map { it.toOrderDto() }
    }

    suspend fun createOrder(
        userId: String,
        salonId: String,
        items: List<OrderItemRequest>,
        shippingAddress: String?,
        paymentMethod: String?,
        specialInstructions: String?,
        promotionCode: String?,
        idempotencyKey: String?
    ): OrderCreationResult = newSuspendedTransaction {
        try {
            // Validate salonId - if "default_salon" or invalid, try to use the first one
            val finalSalonId = if (salonId == "default_salon" || SalonsTable.selectAll().where { SalonsTable.id eq salonId }.empty()) {
                SalonsTable.selectAll().firstOrNull()?.get(SalonsTable.id) ?: salonId
            } else {
                salonId
            }

            // Validate stock for all items
            val stockUpdates = mutableListOf<Pair<String, Int>>()
            for (item in items) {
                val product = ProductsTable.selectAll().where { ProductsTable.id eq item.productId }
                    .singleOrNull() ?: throw Exception("Product not found: ${item.productId}")
                
                val currentStock = product[ProductsTable.stockQuantity]
                if (currentStock < item.quantity) {
                    return@newSuspendedTransaction OrderCreationResult.Error("Insufficient stock for product: ${product[ProductsTable.name]}")
                }
                stockUpdates.add(item.productId to (currentStock - item.quantity))
            }

            // Reduce stock
            stockUpdates.forEach { (productId, newStock) ->
                ProductsTable.update({ ProductsTable.id eq productId }) {
                    it[ProductsTable.stockQuantity] = newStock
                }
            }

            val orderId = UUID.randomUUID().toString()
            val total = items.sumOf { (it.pricePerUnit ?: 0.0) * it.quantity }
            
            meterRegistry.counter("orders_created_total", "salon_id", finalSalonId).increment()
            meterRegistry.summary("orders_revenue_amount", "salon_id", finalSalonId).record(total)

            // Record inventory updates
            stockUpdates.forEach { (productId, newStock) ->
                val invEvent = DomainEvent.InventoryUpdated(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    actorId = userId,
                    message = "Stock reduced for order $orderId",
                    productId = productId,
                    newStock = newStock
                )
                outboxRepository.save(userId, json.encodeToString(invEvent))
            }

            OrdersTable.insert {
                it[OrdersTable.id] = orderId
                it[OrdersTable.userId] = userId
                it[OrdersTable.salonId] = finalSalonId
                it[OrdersTable.totalAmount] = total
                it[OrdersTable.status] = OrderRepoStatus.PENDING.name
                it[OrdersTable.createdAt] = System.currentTimeMillis()
                it[OrdersTable.shippingAddress] = shippingAddress
                it[OrdersTable.paymentMethod] = paymentMethod
                it[OrdersTable.specialInstructions] = specialInstructions
                it[OrdersTable.promoCode] = promotionCode
                it[OrdersTable.idempotencyKey] = idempotencyKey
                it[OrdersTable.expiresAt] = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
            }

            // Record promotion usage if a promo code was used
            if (!promotionCode.isNullOrBlank()) {
                val promo = PromotionsTable.selectAll().where { PromotionsTable.code eq promotionCode }.singleOrNull()
                if (promo != null) {
                    promotionRepository.recordPromoUsage(promo[PromotionsTable.id], userId, orderId)
                }
            }

            items.forEach { item ->
                OrderItemsTable.insert {
                    it[OrderItemsTable.id] = UUID.randomUUID().toString()
                    it[OrderItemsTable.orderId] = orderId
                    it[OrderItemsTable.productId] = item.productId
                    it[OrderItemsTable.quantity] = item.quantity
                    it[OrderItemsTable.price] = item.pricePerUnit ?: 0.0
                    val providerName = ProductsTable.select(ProductsTable.providerName)
                            .where { ProductsTable.id eq item.productId }
                            .singleOrNull()?.get(ProductsTable.providerName) ?: "Mira Store"
                    it[OrderItemsTable.providerName] = providerName
                }
            }

            val event = DomainEvent.OrderCreated(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                actorId = userId,
                message = "Order created",
                orderId = orderId,
                totalAmount = total
            )
            outboxRepository.save(userId, json.encodeToString(event))

            OrderCreationResult.Success(orderId)
        } catch (e: Exception) {
            log.error("Failed to create order for user $userId. Error: ${e.message}", e)
            OrderCreationResult.Error(e.message ?: "Unknown error")
        }
    }

    fun findOrderById(id: String, userId: String? = null): OrderDto? = transaction {
        val query = (OrdersTable leftJoin UsersTable)
            .selectAll().where { OrdersTable.id eq id }
        
        userId?.let { query.andWhere { OrdersTable.userId eq it } }
        
        query.map { it.toOrderDto() }.singleOrNull()
    }

    fun findAllOrdersPaginated(
        page: Int,
        pageSize: Int,
        status: OrderRepoStatus? = null,
        query: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null
    ): PagedResponse<OrderDto> = transaction {
        val baseQuery = (OrdersTable leftJoin UsersTable).selectAll()

        status?.let {
            if (it == OrderRepoStatus.DELIVERED) {
                baseQuery.andWhere { (OrdersTable.status eq "DELIVERED") or (OrdersTable.status eq "COMPLETED") }
            } else {
                baseQuery.andWhere { OrdersTable.status eq it.name }
            }
        }

        query?.let { q ->
            val searchTerm = "%${q.lowercase()}%"
            baseQuery.andWhere {
                (OrdersTable.id.lowerCase() like searchTerm) or
                (UsersTable.name.lowerCase() like searchTerm) or
                (UsersTable.firstName.lowerCase() like searchTerm) or
                (UsersTable.lastName.lowerCase() like searchTerm) or
                (OrdersTable.promoCode.lowerCase() like searchTerm)
            }
        }

        dateFrom?.let { baseQuery.andWhere { OrdersTable.createdAt greaterEq it } }
        dateTo?.let { baseQuery.andWhere { OrdersTable.createdAt lessEq it } }

        val total = baseQuery.count()
        log.info("findAllOrdersPaginated: total={}, status={}, query={}, dateFrom={}, dateTo={}", total, status, query, dateFrom, dateTo)
        val orderRows = baseQuery
            .orderBy(OrdersTable.createdAt to SortOrder.DESC)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .toList()
        
        val orderIds = orderRows.map { it[OrdersTable.id] }
        val itemsMap = if (orderIds.isNotEmpty()) {
            val rows = OrderItemsTable.join(ProductsTable, JoinType.LEFT, OrderItemsTable.productId, ProductsTable.id)
                .select(
                    OrderItemsTable.id,
                    OrderItemsTable.orderId,
                    OrderItemsTable.productId,
                    OrderItemsTable.quantity,
                    OrderItemsTable.price,
                    ProductsTable.name,
                    ProductsTable.imageUrl,
                    ProductsTable.providerName
                )
                .where { OrderItemsTable.orderId inList orderIds }
                .toList()
            log.info("findAllOrdersPaginated: fetched {} items for {} orders", rows.size, orderIds.size)
            rows.groupBy { it[OrderItemsTable.orderId] }
                .mapValues { (_, rows) ->
                    rows.map { 
                        OrderItemDto(
                            id = it[OrderItemsTable.id],
                            productId = it[OrderItemsTable.productId],
                            productName = it.getOrNull(ProductsTable.name) ?: "Unknown Product",
                            productImageUrl = it.getOrNull(ProductsTable.imageUrl),
                            providerName = it.getOrNull(ProductsTable.providerName),
                            quantity = it[OrderItemsTable.quantity],
                            price = it[OrderItemsTable.price].let { p -> if (p.isNaN() || p.isInfinite()) 0.0 else p }
                        )
                    }
                }
        } else {
            emptyMap()
        }

        val items = orderRows.map { row ->
            val orderId = row[OrdersTable.id]
            val orderItems = itemsMap[orderId]
            if (orderItems == null) {
                log.warn("findAllOrdersPaginated: No items found in map for order {}. Fallback will be used.", orderId)
            }
            row.toOrderDto(orderItems)
        }
        
        val totalPages = if (pageSize > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
        PagedResponse(items, total, page, pageSize, totalPages)
    }

    fun updateOrderStatus(id: String, status: OrderRepoStatus): OrderStatusUpdateResult = transaction {
        try {
            val order = OrdersTable.selectAll().where { OrdersTable.id eq id }.singleOrNull()
                ?: return@transaction OrderStatusUpdateResult.NotFound

            val currentStatus = try {
                OrderRepoStatus.valueOf(order[OrdersTable.status].uppercase())
            } catch (e: Exception) {
                OrderRepoStatus.PENDING
            }

            if (currentStatus == status) return@transaction OrderStatusUpdateResult.Success

            // Handle stock restoration if order is being cancelled
            if (status == OrderRepoStatus.CANCELLED) {
                val items = OrderItemsTable.selectAll().where { OrderItemsTable.orderId eq id }
                items.forEach { itemRow ->
                    val productId = itemRow[OrderItemsTable.productId]
                    val qty = itemRow[OrderItemsTable.quantity]
                    ProductsTable.update({ ProductsTable.id eq productId }) {
                        it[stockQuantity] = ProductsTable.stockQuantity.plus(qty)
                    }
                }
            }
            // Handle stock reduction if order is being moved FROM cancelled (rare but possible)
            else if (currentStatus == OrderRepoStatus.CANCELLED) {
                val items = OrderItemsTable.selectAll().where { OrderItemsTable.orderId eq id }
                for (itemRow in items) {
                    val productId = itemRow[OrderItemsTable.productId]
                    val qty = itemRow[OrderItemsTable.quantity]
                    val product = ProductsTable.selectAll().where { ProductsTable.id eq productId }.singleOrNull()
                    if (product == null || product[ProductsTable.stockQuantity] < qty) {
                        return@transaction OrderStatusUpdateResult.DatabaseError(Exception("Insufficient stock to restore order"))
                    }
                    ProductsTable.update({ ProductsTable.id eq productId }) {
                        it[stockQuantity] = ProductsTable.stockQuantity.minus(qty)
                    }
                }
            }

            val updated = OrdersTable.update({ OrdersTable.id eq id }) {
                it[OrdersTable.status] = status.name
            }
            if (updated > 0) {
                val updateEvent = DomainEvent.OrderUpdated(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    actorId = order[OrdersTable.userId],
                    message = "Order status updated to ${status.name}",
                    orderId = id,
                    status = status.name
                )
                outboxRepository.save(order[OrdersTable.userId], json.encodeToString(updateEvent))
                
                // If it was cancelled, also send inventory updates for restored items
                if (status == OrderRepoStatus.CANCELLED) {
                    val items = OrderItemsTable.selectAll().where { OrderItemsTable.orderId eq id }
                    items.forEach { itemRow ->
                        val productId = itemRow[OrderItemsTable.productId]
                        val product = ProductsTable.selectAll().where { ProductsTable.id eq productId }.singleOrNull()
                        if (product != null) {
                            val invEvent = DomainEvent.InventoryUpdated(
                                eventId = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                actorId = "system",
                                message = "Stock restored for cancelled order $id",
                                productId = productId,
                                newStock = product[ProductsTable.stockQuantity]
                            )
                            outboxRepository.save(null, json.encodeToString(invEvent))
                        }
                    }
                }
                OrderStatusUpdateResult.Success
            } else OrderStatusUpdateResult.NotFound
        } catch (e: Exception) {
            OrderStatusUpdateResult.DatabaseError(e)
        }
    }

    fun findByUserPaginated(userId: String, page: Int, pageSize: Int, status: OrderRepoStatus?): PagedResponse<OrderDto> = transaction {
        val query = (OrdersTable leftJoin UsersTable)
            .selectAll().where { OrdersTable.userId eq userId }
        
        status?.let { query.andWhere { OrdersTable.status eq it.name } }
        
        val total = query.count()
        val items = query.orderBy(OrdersTable.createdAt to SortOrder.DESC)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map { it.toOrderDto() }
        
        val totalPages = if (pageSize > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
        PagedResponse(items, total, page, pageSize, totalPages)
    }

    fun cleanupExpiredOrders() = transaction {
        val now = System.currentTimeMillis()
        val expiredOrders = OrdersTable.selectAll().where {
            (OrdersTable.expiresAt lessEq now) and (OrdersTable.status eq OrderRepoStatus.PENDING.name)
        }.map { it[OrdersTable.id] }
        
        expiredOrders.forEach { orderId ->
            updateOrderStatus(orderId, OrderRepoStatus.CANCELLED)
            log.info("Cancelled expired order: {}", orderId)
        }
        
        expiredOrders.size
    }

    // --- CoreOrderRepository implementation ---

    override fun observeOrders(): Flow<Outcome<List<Order>>> = flow {
        emit(fetchOrders())
    }

    override suspend fun fetchOrders(): Outcome<List<Order>> = transaction {
        Outcome.Success(
            (OrdersTable leftJoin UsersTable)
                .selectAll()
                .map { it.toDomainOrder() }
        )
    }

    override suspend fun placeOrder(order: Order): Outcome<String> {
        // Mapping domain Order back to server-side creation
        val result = createOrder(
            userId = order.userId,
            salonId = "", // Should be part of Order or provided context
            items = order.items.map { it.toOrderItemRequest() },
            shippingAddress = null,
            paymentMethod = null,
            specialInstructions = null,
            promotionCode = order.promoCode,
            idempotencyKey = null
        )
        return when (result) {
            is OrderCreationResult.Success -> Outcome.Success(result.orderId)
            is OrderCreationResult.Error -> Outcome.Error(Failure.ServerError(500, result.message))
        }
    }

    override suspend fun getOrderDetails(orderId: String): Outcome<Order> = transaction {
        OrdersTable.selectAll().where { OrdersTable.id eq orderId }
            .map { it.toDomainOrder() }
            .singleOrNull()?.let { Outcome.Success(it) } 
            ?: Outcome.Error(Failure.ServerError(404, "Order not found"))
    }

    override suspend fun deleteOrder(orderId: String): Outcome<Unit> = transaction {
        try {
            OrderItemsTable.deleteWhere { OrderItemsTable.orderId eq orderId }
            val deletedCount = OrdersTable.deleteWhere { OrdersTable.id eq orderId }
            if (deletedCount > 0) {
                Outcome.Success(Unit)
            } else {
                Outcome.Error(Failure.ServerError(404, "Order not found"))
            }
        } catch (e: Exception) {
            Outcome.Error(Failure.ServerError(500, e.message ?: "Database error"))
        }
    }

    // --- Mappers ---

    private fun ResultRow.toOrderDto(items: List<OrderItemDto>? = null): OrderDto {
        val orderId = this[OrdersTable.id]
        val resolvedItems = items ?: transaction {
            OrderItemsTable.join(ProductsTable, JoinType.LEFT, OrderItemsTable.productId, ProductsTable.id)
                .select(
                    OrderItemsTable.id,
                    OrderItemsTable.orderId,
                    OrderItemsTable.productId,
                    OrderItemsTable.quantity,
                    OrderItemsTable.price,
                    ProductsTable.name,
                    ProductsTable.imageUrl,
                    ProductsTable.providerName
                )
                .where { OrderItemsTable.orderId eq orderId }
                .map {
                    OrderItemDto(
                        id = it[OrderItemsTable.id],
                        productId = it[OrderItemsTable.productId],
                        productName = it.getOrNull(ProductsTable.name) ?: "Unknown Product",
                        productImageUrl = it.getOrNull(ProductsTable.imageUrl),
                        providerName = it.getOrNull(ProductsTable.providerName),
                        quantity = it[OrderItemsTable.quantity],
                        price = it[OrderItemsTable.price]
                    )
                }
        }

        val statusStr = this[OrdersTable.status]
        val statusEnum = when (statusStr.uppercase()) {
            "COMPLETED", "DELIVERED" -> OrderStatusDto.DELIVERED
            else -> try {
                OrderStatusDto.valueOf(statusStr.uppercase())
            } catch (e: Exception) {
                log.warn("Invalid order status in DB for order {}: {}", orderId, statusStr)
                OrderStatusDto.PENDING
            }
        }

        val firstName = this.getOrNull(UsersTable.firstName)
        val lastName = this.getOrNull(UsersTable.lastName)
        val dbName = this.getOrNull(UsersTable.name)
        val userRole = this.getOrNull(UsersTable.role)
        
        val resolvedName = when {
            !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
            !firstName.isNullOrBlank() -> firstName
            !lastName.isNullOrBlank() -> lastName
            !dbName.isNullOrBlank() -> dbName
            else -> "Guest Customer"
        }
        
        val userId = this[OrdersTable.userId]
        log.info("toOrderDto: orderId={}, userId={}, resolvedName={}, userRole={}, rawDbName={}", 
            orderId, userId, resolvedName, userRole, dbName)

        val totalAmount = this[OrdersTable.totalAmount].let { if (it.isNaN() || it.isInfinite()) 0.0 else it }
        val discountAmount = this[OrdersTable.discountAmount].let { if (it.isNaN() || it.isInfinite()) 0.0 else it }

        val userEmail = this.getOrNull(UsersTable.email)
        val userPhone = this.getOrNull(UsersTable.phone)

        return OrderDto(
            id = orderId,
            userId = userId,
            userName = resolvedName,
            userEmail = userEmail,
            userPhone = userPhone,
            items = resolvedItems,
            subtotalAmount = totalAmount - discountAmount,
            taxAmount = 0.0,
            shippingFees = 0.0,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            status = statusEnum,
            createdAt = this[OrdersTable.createdAt],
            updatedAt = this[OrdersTable.createdAt],
            promoCode = this[OrdersTable.promoCode],
            expiresAt = this[OrdersTable.expiresAt]?.let { it / 1000 },
            shippingAddress = this[OrdersTable.shippingAddress],
            paymentMethod = this[OrdersTable.paymentMethod],
            specialInstructions = this[OrdersTable.specialInstructions],
            trackingNumber = this[OrdersTable.trackingNumber]
        )
    }

    private fun ResultRow.toDomainOrder(): Order {
        val statusStr = this[OrdersTable.status]
        val statusEnum = try {
            OrderStatus.valueOf(statusStr.uppercase())
        } catch (e: Exception) {
            OrderStatus.PENDING
        }
        
        val firstName = this.getOrNull(UsersTable.firstName) ?: ""
        val lastName = this.getOrNull(UsersTable.lastName) ?: ""
        val email = this.getOrNull(UsersTable.email) ?: ""
        
        return Order(
            id = this[OrdersTable.id],
            userId = this[OrdersTable.userId],
            items = emptyList(), // Items should be fetched separately if needed
            total = this[OrdersTable.totalAmount],
            status = statusEnum,
            placedAtEpochSeconds = this[OrdersTable.createdAt] / 1000,
            promoCode = this[OrdersTable.promoCode],
            expiresAt = this[OrdersTable.expiresAt]?.let { it / 1000 },
            userName = "$firstName $lastName".trim().ifEmpty { "Guest Customer" },
            userEmail = email,
        )
    }

    private fun CartItem.toOrderItemRequest() = OrderItemRequest(
        productId = this.product.id,
        quantity = this.quantity,
        pricePerUnit = this.product.discountedPrice
    )
}
