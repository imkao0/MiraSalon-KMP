package iz.mkao.mirasalon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "specialists")
data class SpecialistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val title: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val salonId: String? = null,
    val customersCount: Int = 0,
    val yearsOfExperience: Int = 0,
    val isOnline: Boolean = false,
    val userId: String? = null
)
