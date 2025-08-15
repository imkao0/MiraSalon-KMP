package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Promotion(
    val id: String? = null,
    val title: String = "",
    val ctaText: String? = null,
    val discountDescription: String = "",
    val code: String? = null,
    val imageUrl: String? = null,
    val discountPercent: Int = 0,
    val isActive: Boolean = true,
    
    // Extended fields for server logic
    val description: String = "",
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountValue: Double = 0.0,
    val validFrom: Instant? = null,
    val validUntil: Instant? = null,
    val usageLimit: UsageLimit = UsageLimit(),
    val currentUsageCount: Int = 0,
    val minOrderValue: Double? = null,
    val applicableServices: List<String>? = null,
    val applicableCategories: List<String>? = null,
    val targetUserId: String? = null,
    val isFirstPurchaseOnly: Boolean = false,
    val stackable: Boolean = false,
    val status: PromoStatus = PromoStatus.ACTIVE,
    val type: PromoType = PromoType.EXPERTS
)

@Serializable
enum class PromoType {
    EXPERTS,
    HAIR_COLOR,
    SPECIALIST_MATCH
}

@Serializable
enum class DiscountType {
    PERCENTAGE,
    FIXED,
    FREE_SERVICE
}

@Serializable
enum class PromoStatus {
    ACTIVE,
    INACTIVE,
    SCHEDULED,
    EXPIRED,
    PAUSED
}

@Serializable
data class UsageLimit(
    val totalRedemptions: Int? = null,
    val perUserRedemptions: Int = 1
)
