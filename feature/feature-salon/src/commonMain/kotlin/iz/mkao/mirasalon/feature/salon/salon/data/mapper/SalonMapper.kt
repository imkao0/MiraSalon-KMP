package iz.mkao.mirasalon.feature.salon.salon.data.mapper

import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.model.Salon
import iz.mkao.mirasalon.core.domain.model.SalonCategory
import iz.mkao.mirasalon.core.domain.model.SalonHome
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.PromotionDto
import iz.mkao.mirasalon.core.network.model.dto.SalonDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceCategoryDto
import iz.mkao.mirasalon.core.network.model.dto.SalonHomeDto
import iz.mkao.mirasalon.feature.specialists.data.mapper.toDomain

fun ServiceCategoryDto.toDomain(): SalonCategory = SalonCategory(
    id = id,
    name = name,
    iconName = ApiEndpoints.resolveImageUrl(iconUrl) ?: "",
)

fun SalonDto.toDomain(): Salon = Salon(
    id = id,
    name = name,
    address = address,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    rating = rating,
    openTime = openTime ?: "08:00",
    closeTime = closeTime ?: "20:00",
    timezoneId = timezoneId ?: "UTC"
)

fun PromotionDto.toDomain(): Promotion = Promotion(
    id = id,
    title = title,
    description = description,
    discountDescription = discountDescription,
    ctaText = ctaText,
    discountPercent = if (discountType == "PERCENTAGE") discountValue.toInt() else 0,
    code = code,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    isActive = status == "ACTIVE"
)

fun SalonHomeDto.toDomain(): SalonHome = SalonHome(
    categories = categories.map { it.toDomain() },
    specialists = specialists.map { it.toDomain() },
    promotions = promotions.map { it.toDomain() },
    isLoggedIn = isLoggedIn,
)
