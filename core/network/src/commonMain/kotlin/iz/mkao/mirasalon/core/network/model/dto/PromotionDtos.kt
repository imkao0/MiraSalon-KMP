package iz.mkao.mirasalon.core.network.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class PromotionDto(
    val id: String,
    val code: String,
    val title: String = "",
    val ctaText: String? = null,
    val discountDescription: String = "",
    val description: String,
    val discountType: String,
    val discountValue: Double,
    val validFrom: Long,
    val validUntil: Long,
    val totalRedemptions: Int? = null,
    val perUserRedemptions: Int = 1,
    val usageLimit: Int = 0,
    val currentUsageCount: Int = 0,
    val minOrderValue: Double? = null,
    val applicableServices: List<String>? = null,
    val applicableCategories: List<String>? = null,
    val targetUserId: String? = null,
    val isFirstPurchaseOnly: Boolean = false,
    val stackable: Boolean = false,
    val status: String = "ACTIVE",
    val imageUrl: String? = null,
    val type: String? = null
)

@Serializable
data class CreatePromotionRequestDto(
    val code: String,
    val title: String = "",
    val ctaText: String? = null,
    val description: String,
    val discountType: String,
    val discountValue: Double,
    val validFrom: Long,
    val validUntil: Long,
    val totalRedemptions: Int? = null,
    val perUserRedemptions: Int = 1,
    val minOrderValue: Double? = null,
    val applicableServices: List<String>? = null,
    val applicableCategories: List<String>? = null,
    val targetUserId: String? = null,
    val isFirstPurchaseOnly: Boolean = false,
    val stackable: Boolean = false,
    val status: String = "ACTIVE",
    val imageUrl: String? = null,
    val type: String? = null
)

@Serializable
data class UpdatePromotionRequestDto(
    val code: String? = null,
    val title: String? = null,
    val ctaText: String? = null,
    val description: String? = null,
    val discountType: String? = null,
    val discountValue: Double? = null,
    val validFrom: Long? = null,
    val validUntil: Long? = null,
    val totalRedemptions: Int? = null,
    val perUserRedemptions: Int? = null,
    val minOrderValue: Double? = null,
    val applicableServices: List<String>? = null,
    val applicableCategories: List<String>? = null,
    val targetUserId: String? = null,
    val isFirstPurchaseOnly: Boolean? = null,
    val stackable: Boolean? = null,
    val status: String? = null,
    val imageUrl: String? = null,
    val type: String? = null
)
