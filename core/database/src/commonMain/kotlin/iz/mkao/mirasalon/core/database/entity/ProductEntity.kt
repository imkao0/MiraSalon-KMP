package iz.mkao.mirasalon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val price: Double,
    val stockQuantity: Int,
    val discountPercent: Int,
    val averageRating: Double,
    val reviewCount: Int,
    val providerName: String = "Mira Store",
    val isActive: Boolean
)
