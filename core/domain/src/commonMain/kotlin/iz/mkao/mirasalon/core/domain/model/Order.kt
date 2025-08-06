package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable

data class Order(
    val id: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userPhone: String? = null,
    val items: List<CartItem>,
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val shippingFees: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double,
    val status: OrderStatus = OrderStatus.PENDING,
    val placedAtEpochSeconds: Long,
    val promoCode: String? = null,
    val shippingAddress: String? = null,
    val paymentMethod: String? = null,
    val trackingNumber: String? = null,
    val specialInstructions: String? = null,
    val expiresAt: Long? = null
)
