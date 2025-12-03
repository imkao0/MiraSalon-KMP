package iz.mkao.mirasalon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_favorites")
data class ProductFavoriteEntity(
    @PrimaryKey val productId: String,
    val name: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val price: Double,
    val stockQuantity: Int,
    val discountPercent: Int,
    val averageRating: Double,
    val reviewCount: Int,
    val gender: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0L
)
