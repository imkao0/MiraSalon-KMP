package iz.mkao.mirasalon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "order_items",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItemEntity(
    val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val productImageUrl: String? = null,
    val providerName: String? = "Mira Store",
    val quantity: Int,
    val price: Double
)
