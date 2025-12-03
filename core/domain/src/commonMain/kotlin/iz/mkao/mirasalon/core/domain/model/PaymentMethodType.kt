package iz.mkao.mirasalon.core.domain.model

enum class PaymentMethodType(val displayName: String, val isAvailable: Boolean) {
    CASH("Cash", isAvailable = true),
    CASH_ON_DELIVERY("Cash on Delivery", isAvailable = true),
    CARD("Credit / Debit Card", isAvailable = true),
    VISA("Visa", isAvailable = true),
    MASTER_CARD("Master Card", isAvailable = true),
    GOOGLE_PAY("Google Pay", isAvailable = true),
    APPLE_PAY("Apple Pay", isAvailable = true),
    RAZORPAY("Razorpay", isAvailable = false),
}
