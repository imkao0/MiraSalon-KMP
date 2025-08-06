package iz.mkao.mirasalon.feature.cart.data.mapper

import iz.mkao.mirasalon.core.database.dao.OrderWithItems
import iz.mkao.mirasalon.core.database.entity.OrderEntity
import iz.mkao.mirasalon.core.database.entity.OrderItemEntity
import iz.mkao.mirasalon.core.domain.model.*
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.OrderDto
import iz.mkao.mirasalon.core.network.model.dto.OrderItemDto
import iz.mkao.mirasalon.core.network.model.dto.OrderStatusDto

fun OrderDto.toDomain(): Order = Order(
    id = id,
    userId = userId,
    userName = userName ?: "",
    userEmail = userEmail ?: "",
    userPhone = userPhone,
    items = items.map { it.toDomain() },
    subtotal = subtotalAmount,
    tax = taxAmount,
    shippingFees = shippingFees,
    discount = discountAmount,
    total = totalAmount,
    status = status.toDomain(),
    placedAtEpochSeconds = createdAt,
    promoCode = promoCode,
    shippingAddress = shippingAddress,
    paymentMethod = paymentMethod,
    trackingNumber = trackingNumber,
    specialInstructions = specialInstructions,

)

fun OrderItemDto.toDomain(): CartItem = CartItem(
    product = Product(
        id = productId,
        name = productName ?: "Unknown Product",
        category = "",
        description = "",
        imageUrl = ApiEndpoints.resolveImageUrl(productImageUrl) ?: "",
        price = price,
        providerName = providerName ?: "Mira Store",
        stockQuantity = 0
    ),
    quantity = quantity,

)

fun OrderStatusDto.toDomain(): OrderStatus = when (this) {
    OrderStatusDto.PENDING -> OrderStatus.PENDING
    OrderStatusDto.SHIPPED -> OrderStatus.SHIPPED
    OrderStatusDto.REFUNDED -> OrderStatus.REFUNDED
    OrderStatusDto.DELIVERED -> OrderStatus.DELIVERED
    OrderStatusDto.CANCELLED -> OrderStatus.CANCELLED
}

fun CartItem.toRequest(): iz.mkao.mirasalon.core.network.model.dto.OrderItemRequest = iz.mkao.mirasalon.core.network.model.dto.OrderItemRequest(
    productId = product.id,
    quantity = quantity,
    pricePerUnit = product.discountedPrice
)

fun OrderDto.toEntity() = OrderEntity(
    id = id,
    userId = userId,
    userName = userName ?: "Unknown",
    userEmail = userEmail ?: "unknown@example.com",
    userPhone = userPhone,
    subtotalAmount = subtotalAmount,
    taxAmount = taxAmount,
    shippingFees = shippingFees,
    totalAmount = totalAmount,
    discountAmount = discountAmount,
    status = status.name,
    promoCode = promoCode,
    createdAt = createdAt,
    expiresAt = expiresAt,
    shippingAddress = shippingAddress,
    paymentMethod = paymentMethod
)

fun OrderItemDto.toEntity(orderId: String) = OrderItemEntity(
    id = id ?: "", 
    orderId = orderId,
    productId = productId,
    productName = productName ?: "Unknown",
    productImageUrl = productImageUrl,
    providerName = providerName,
    quantity = quantity,
    price = price
)

fun OrderWithItems.toDomain(): Order {
    val o = order
    return Order(
        id = o.id,
        userId = o.userId,
        userName = o.userName,
        userEmail = o.userEmail,
        userPhone = o.userPhone,
        items = items.map { 
            CartItem(
                product = Product(
                    id = it.productId,
                    name = it.productName,
                    category = "",
                    description = "",
                    imageUrl = ApiEndpoints.resolveImageUrl(it.productImageUrl) ?: "",
                    price = it.price,
                    providerName = it.providerName ?: "Mira Store",
                    stockQuantity = 0
                ),
                quantity = it.quantity
            )
        },
        subtotal = o.subtotalAmount,
        tax = o.taxAmount,
        shippingFees = o.shippingFees,
        discount = o.discountAmount,
        total = o.totalAmount,
        status = try { OrderStatus.valueOf(o.status) } catch(e: Exception) { OrderStatus.PENDING },
        placedAtEpochSeconds = o.createdAt / 1000,
        promoCode = o.promoCode,
        shippingAddress = o.shippingAddress,
        paymentMethod = o.paymentMethod
    )
}
