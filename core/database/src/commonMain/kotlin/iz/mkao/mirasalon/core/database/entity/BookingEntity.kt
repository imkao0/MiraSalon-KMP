package iz.mkao.mirasalon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String? = null,
    val userEmail: String? = null,
    val salonId: String,
    val salonName: String,
    val salonAddress: String? = null,
    val salonImageUrl: String? = null,
    val specialistId: String,
    val specialistName: String,
    val specialistAvatarUrl: String? = null,
    val status: String,
    val dateTime: Long,
    val durationMinutes: Int,
    val subtotalAmount: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val reminderEnabled: Boolean,
    val isReviewed: Boolean = false
)
