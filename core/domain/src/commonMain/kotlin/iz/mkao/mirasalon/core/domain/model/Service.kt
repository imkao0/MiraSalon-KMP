package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable

data class Service(
    val id: String,
    val name: String,
    val description: String = "",
    val durationMinutes: Int,
    val price: Double,
    val categoryId: String = "",
    val subCategory: String? = null,
    val discountPercent: Int = 0,
    val imageUrl: String? = null,
    val rating: Double = 0.0,
    val reviews: List<Review> = emptyList()
) {
    val discountedPrice: Double
        get() = if (discountPercent > 0) {
            price * (1.0 - (discountPercent / 100.0))
        } else {
            price
        }
}
