package iz.mkao.mirasalon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "booking_services",
    primaryKeys = ["bookingId", "serviceId"],
    foreignKeys = [
        ForeignKey(
            entity = BookingEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookingId")]
)
data class BookingServiceEntity(
    val bookingId: String,
    val serviceId: String,
    val name: String,
    val price: Double
)
