package iz.mkao.mirasalon.feature.profile.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodDto(
    val id: String,
    val type: String,
    val label: String,
    val last4Digits: String? = null,
    val isDefault: Boolean = false,
)
