package iz.mkao.mirasalon.server.domain.promotion

import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.network.model.dto.PromotionDto

fun Promotion.toDto(): PromotionDto {
    return PromotionDto(
        id = id ?: "",
        code = code ?: "",
        title = title,
        ctaText = ctaText,
        description = description,
        discountType = discountType.name,
        discountValue = discountValue,
        validFrom = validFrom?.toEpochMilliseconds() ?: 0L,
        validUntil = validUntil?.toEpochMilliseconds() ?: 0L,
        totalRedemptions = usageLimit.totalRedemptions,
        perUserRedemptions = usageLimit.perUserRedemptions,
        usageLimit = usageLimit.totalRedemptions ?: 0,
        currentUsageCount = currentUsageCount,
        minOrderValue = minOrderValue,
        applicableServices = applicableServices,
        applicableCategories = applicableCategories,
        targetUserId = targetUserId,
        isFirstPurchaseOnly = isFirstPurchaseOnly,
        stackable = stackable,
        status = status.name,
        imageUrl = if (imageUrl != null) "/v1/api/promotions/$id/image" else null
    )
}
