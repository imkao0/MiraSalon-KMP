package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.domain.model.*
import iz.mkao.mirasalon.core.network.model.dto.*

fun OrderDto.toDomain(): AdminOrder = AdminOrder(
    id = id,
    customerId = userId,
    customerName = userName ?: "Customer",
    items = items.map { it.toDomain() },
    totalAmount = totalAmount,
    status = status.toDomain(),
    paymentMethod = OrderPaymentMethod.fromString(paymentMethod),
    createdAt = createdAt,
    promoCode = promoCode
)

fun OrderItemDto.toDomain(): AdminOrderItem = AdminOrderItem(
    productId = productId,
    productName = productName ?: "Product",
    quantity = quantity,
    unitPrice = price
)

fun OrderStatusDto.toDomain(): AdminOrderStatus = when (this) {
    OrderStatusDto.PENDING -> AdminOrderStatus.Pending
    OrderStatusDto.SHIPPED -> AdminOrderStatus.Shipped
    OrderStatusDto.REFUNDED -> AdminOrderStatus.Refunded
    OrderStatusDto.DELIVERED -> AdminOrderStatus.Delivered
    OrderStatusDto.CANCELLED -> AdminOrderStatus.Cancelled
}
