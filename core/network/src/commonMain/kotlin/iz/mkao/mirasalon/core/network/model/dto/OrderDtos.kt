package iz.mkao.mirasalon.core.network.model.dto

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatusDto {
    PENDING, SHIPPED, REFUNDED, DELIVERED, CANCELLED
}

@Serializable
data class OrderDto(
    val id: String,
    val userId: String,
    val userName: String? = null,
    val userEmail: String? = null,
    val userPhone: String? = null,
    val items: List<OrderItemDto> = emptyList(),
    val subtotalAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val shippingFees: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: OrderStatusDto,
    val promoCode: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val expiresAt: Long? = null,
    val shippingAddress: String? = null,
    val paymentMethod: String? = null,
    val specialInstructions: String? = null,
    val trackingNumber: String? = null
)

@Serializable
data class OrderItemDto(
    val id: String,
    val productId: String,
    val productName: String? = null,
    val productImageUrl: String? = null,
    val providerName: String? = "Mira Store",
    val quantity: Int,
    val price: Double = 0.0,
    val variantId: String? = null
)

@Serializable
data class CreateOrderRequest(
    val items: List<OrderItemRequest>,
    val promoCode: String? = null,
    val totalAmount: Double = 0.0,
    val salonId: String? = null,
    val shippingAddress: String? = null,
    val paymentMethod: String? = null,
    val specialInstructions: String? = null
)

@Serializable
data class OrderItemRequest(
    val productId: String,
    val quantity: Int,
    val variantId: String? = null,
    val variantName: String? = null,
    val pricePerUnit: Double? = null
)

@Serializable
data class PromoValidationRequest(
    val code: String,
    val productIds: List<String>
)

@Serializable
data class PromoValidationDto(
    val promoCode: String,
    val isValid: Boolean,
    val discountAmount: Double = 0.0,
    val applicableServices: List<String>? = null,
    val errorMessage: String? = null
)

@Serializable
data class PromoValidationResult(
    val valid: Boolean,
    val discount: Double = 0.0,
    val discountType: String? = null,
    val discountValue: Double = 0.0,
    val finalTotal: Double = 0.0,
    val message: String? = null,
    val error: String? = null,
    val code: String? = null,
    val applicableServices: List<String>? = null
)

@Serializable
data class ValidatePromoRequest(
    val code: String,
    val cartTotal: Double = 0.0,
    val serviceIds: List<String> = emptyList()
)

@Serializable
data class UpdateOrderStatusRequest(
    val status: String
)
