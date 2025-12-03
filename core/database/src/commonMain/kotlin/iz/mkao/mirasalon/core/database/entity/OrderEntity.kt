package iz.mkao.mirasalon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userPhone: String? = null,
    val subtotalAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val shippingFees: Double = 0.0,
    val totalAmount: Double,
    val discountAmount: Double = 0.0,
    val status: String,
    val promoCode: String? = null,
    val createdAt: Long,
    val expiresAt: Long? = null,
    val shippingAddress: String? = null,
    val paymentMethod: String? = null
)
