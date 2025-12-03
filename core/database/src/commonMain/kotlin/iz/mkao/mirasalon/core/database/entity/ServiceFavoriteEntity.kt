package iz.mkao.mirasalon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_favorites")
data class ServiceFavoriteEntity(
    @PrimaryKey val serviceId: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val price: Double,
    val categoryId: String,
    val subCategory: String? = null,
    val discountPercent: Int = 0,
    val imageUrl: String? = null,
    val createdAt: Long = 0L
)
