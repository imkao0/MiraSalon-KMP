package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.domain.model.AdminDiscountType
import iz.mkao.mirasalon.core.domain.model.AdminPromoStatus
import iz.mkao.mirasalon.core.domain.model.AdminPromotion
import iz.mkao.mirasalon.core.domain.model.DiscountType
import iz.mkao.mirasalon.core.domain.model.PromoStatus
import iz.mkao.mirasalon.core.domain.model.PromoType
import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.model.UsageLimit
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.CreatePromotionRequestDto
import iz.mkao.mirasalon.core.network.model.dto.PromotionDto
import iz.mkao.mirasalon.core.network.model.dto.UpdatePromotionRequestDto
import kotlin.time.Instant

fun PromotionDto.toDomain() = AdminPromotion(
    id = id,
    code = code,
    description = description,
    discountType = AdminDiscountType.fromString(discountType),
    discountValue = discountValue,
    minSpend = minOrderValue ?: 0.0,
    validFrom = validFrom,
    validUntil = if (validUntil > 0) validUntil else null,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    isActive = status == "ACTIVE",
    totalRedemptions = totalRedemptions,
    perUserRedemptions = perUserRedemptions,
    applicableServices = applicableServices ?: emptyList(),
    applicableCategories = applicableCategories ?: emptyList(),
    status = AdminPromoStatus.fromString(status),
    currentUsageCount = currentUsageCount,
    minOrderValue = minOrderValue,
    type = when (type?.uppercase()) {
        "HAIR_COLOR" -> PromoType.HAIR_COLOR
        "SPECIALIST_MATCH" -> PromoType.SPECIALIST_MATCH
        else -> PromoType.EXPERTS
    }
)

fun PromotionDto.toClientDomain() = Promotion(
    id = id,
    title = title,
    ctaText = ctaText,
    discountDescription = discountDescription,
    code = code,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    discountPercent = if (discountType == "PERCENTAGE") discountValue.toInt() else 0,
    isActive = status == "ACTIVE",
    description = description,
    discountType = when (discountType.uppercase()) {
        "PERCENTAGE" -> DiscountType.PERCENTAGE
        "FIXED" -> DiscountType.FIXED
        "FREE_SERVICE" -> DiscountType.FREE_SERVICE
        else -> DiscountType.PERCENTAGE
    },
    discountValue = discountValue,
    validFrom = Instant.fromEpochMilliseconds(validFrom),
    validUntil = if (validUntil > 0) Instant.fromEpochMilliseconds(validUntil) else null,
    usageLimit = UsageLimit(
        totalRedemptions = totalRedemptions,
        perUserRedemptions = perUserRedemptions
    ),
    currentUsageCount = currentUsageCount,
    minOrderValue = minOrderValue,
    applicableServices = applicableServices,
    applicableCategories = applicableCategories,
    targetUserId = targetUserId,
    isFirstPurchaseOnly = isFirstPurchaseOnly,
    stackable = stackable,
    status = when (status.uppercase()) {
        "ACTIVE" -> PromoStatus.ACTIVE
        "INACTIVE" -> PromoStatus.INACTIVE
        "SCHEDULED" -> PromoStatus.SCHEDULED
        "EXPIRED" -> PromoStatus.EXPIRED
        else -> PromoStatus.ACTIVE
    },
    type = when (type?.uppercase()) {
        "HAIR_COLOR" -> PromoType.HAIR_COLOR
        "SPECIALIST_MATCH" -> PromoType.SPECIALIST_MATCH
        else -> PromoType.EXPERTS
    }
)

fun AdminPromotion.toCreateDto() = CreatePromotionRequestDto(
    code = code,
    description = description,
    discountType = discountType.name.uppercase(),
    discountValue = discountValue,
    validFrom = validFrom,
    validUntil = validUntil ?: 0L,
    totalRedemptions = totalRedemptions,
    perUserRedemptions = perUserRedemptions,
    minOrderValue = minOrderValue,
    applicableServices = applicableServices,
    applicableCategories = applicableCategories,
    status = status.name.uppercase(),
    imageUrl = imageUrl,
    type = type.name
)

fun AdminPromotion.toUpdateDto() = UpdatePromotionRequestDto(
    code = code,
    description = description,
    discountType = discountType.name.uppercase(),
    discountValue = discountValue,
    validFrom = validFrom,
    validUntil = validUntil,
    totalRedemptions = totalRedemptions,
    perUserRedemptions = perUserRedemptions,
    minOrderValue = minOrderValue,
    applicableServices = applicableServices,
    applicableCategories = applicableCategories,
    status = status.name.uppercase(),
    imageUrl = imageUrl,
    type = type.name
)
