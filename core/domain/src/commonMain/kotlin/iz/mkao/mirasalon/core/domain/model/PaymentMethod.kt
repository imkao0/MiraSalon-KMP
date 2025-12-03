package iz.mkao.mirasalon.core.domain.model

data class PaymentMethod(
    val id: String,
    val type: PaymentMethodType,
    val label: String,
    val last4Digits: String? = null,
    val expiryDate: String? = null,
    val isDefault: Boolean = false,
)
