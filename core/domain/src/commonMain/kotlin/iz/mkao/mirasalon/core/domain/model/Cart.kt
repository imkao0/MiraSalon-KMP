package iz.mkao.mirasalon.core.domain.model

data class Cart(
    val items: List<CartItem> = emptyList(),
    val couponCode: String? = null,
    val discountAmount: Double = 0.0
) {
    val subtotal: Double = items.sumOf { it.product.discountedPrice * it.quantity }
    val total: Double = (subtotal - discountAmount).coerceAtLeast(0.0)
    val itemCount: Int = items.sumOf { it.quantity }
}
