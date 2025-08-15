package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val price: Double,
    val stockQuantity: Int,
    val discountPercent: Int = 0,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val gender: String? = null,
    val providerName: String = "Mira Store",
    val reviews: List<Review> = emptyList(),
    val isActive: Boolean = true,
) {
    val discountedPrice: Double
        get() = if (discountPercent > 0) {
            price * (1.0 - (discountPercent / 100.0))
        } else {
            price
        }
}
