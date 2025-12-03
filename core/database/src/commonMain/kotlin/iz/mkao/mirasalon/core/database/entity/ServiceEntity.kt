package iz.mkao.mirasalon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val price: Double,
    val categoryId: String,
    val subCategory: String? = null,
    val discountPercent: Int = 0,
    val imageUrl: String? = null,
    val rating: Double = 0.0,
    val salonId: String? = null
)
