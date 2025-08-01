package iz.mkao.mirasalon.core.domain.model

/** Lifecycle states for an admin-managed order. */
enum class AdminOrderStatus {
    Pending,
    Shipped,
    Refunded,
    Delivered,
    Cancelled,
    ;

    companion object {
        fun fromString(value: String?): AdminOrderStatus =
            entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: Pending
    }
}

/** How an order was (or will be) paid for. */
enum class OrderPaymentMethod(val type: String, val provider: String) {
    Cash("Cash", "On-site"),
    Card("Credit Card", "Stripe"),
    Online("Online", "Paypal"),
    Wallet("Digital Wallet", "Apple/Google Pay"),
    ;

    companion object {
        val DEFAULT_METHODS = entries
        
        fun fromString(value: String?): OrderPaymentMethod =
            entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: Cash
    }
}

/** A single line item within an [AdminOrder]. */
data class AdminOrderItem(
    val productId: String,
    val productName: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
)

/**
 * Admin-facing order model.
 */
data class AdminOrder(
    val id: String,
    val customerId: String = "",
    val customerName: String = "",
    val items: List<AdminOrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: AdminOrderStatus = AdminOrderStatus.Pending,
    val paymentMethod: OrderPaymentMethod = OrderPaymentMethod.Cash,
    val createdAt: Long = 0L,
    val promoCode: String? = null,
)
