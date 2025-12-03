package iz.mkao.mirasalon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "promotions")
data class PromotionEntity(
    @PrimaryKey val id: String,
    val code: String,
    val description: String,
    val discountType: String,
    val discountValue: Double,
    val imageUrl: String? = null,
    val status: String,
    val validFrom: Long? = null,
    val validUntil: Long? = null,
    val title: String = "",
    val ctaText: String? = null,
    val promoType: String = "EXPERTS"
)
