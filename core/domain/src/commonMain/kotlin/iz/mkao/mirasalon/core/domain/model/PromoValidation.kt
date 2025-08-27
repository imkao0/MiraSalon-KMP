package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PromoValidation(
    val promoCode: String,
    val isValid: Boolean,
    val discountAmount: Double = 0.0,
    val errorMessage: String? = null,
    val applicableServices: List<String>? = null,
    val applicableCategories: List<String>? = null
)
