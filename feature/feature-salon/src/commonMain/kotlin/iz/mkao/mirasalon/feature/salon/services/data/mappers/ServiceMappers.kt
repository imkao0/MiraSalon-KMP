package iz.mkao.mirasalon.feature.salon.services.data.mappers

import iz.mkao.mirasalon.core.database.entity.ServiceCategoryEntity
import iz.mkao.mirasalon.core.database.entity.ServiceEntity
import iz.mkao.mirasalon.core.domain.model.Review
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceCategoryDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto

// --- DTO to Domain ---
fun ServiceCategoryDto.toDomain() = ServiceCategory(
    id = id,
    name = name,
    iconName = iconUrl,
    iconUrl = ApiEndpoints.resolveImageUrl(iconUrl)
)

fun ServiceDto.toDomain() = Service(
    id = id,
    name = name,
    description = description ?: "",
    durationMinutes = durationMinutes,
    price = price,
    discountPercent = discountPercent,
    categoryId = categoryId ?: "",
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    reviews = reviews.map { it.toDomain() }
)

fun ReviewDto.toDomain() = Review(
    id = id,
    userName = userName,
    userAvatarUrl = ApiEndpoints.resolveImageUrl(userAvatarUrl),
    rating = rating,
    comment = comment,
    createdAtEpochSeconds = createdAtEpochSeconds
)

// --- Entity to Domain ---
fun ServiceEntity.toDomain() = Service(
    id = id,
    name = name,
    description = description,
    durationMinutes = durationMinutes,
    price = price,
    discountPercent = discountPercent,
    categoryId = categoryId,
    imageUrl = imageUrl,
    rating = rating
)

fun ServiceCategoryEntity.toDomain() = ServiceCategory(
    id = id,
    name = name,
    iconName = iconName,
    iconUrl = iconUrl
)

// --- DTO to Entity ---
fun ServiceDto.toEntity() = ServiceEntity(
    id = id,
    name = name,
    description = description ?: "",
    durationMinutes = durationMinutes,
    price = price,
    discountPercent = discountPercent,
    categoryId = categoryId ?: "",
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    rating = rating,
    salonId = salonId
)

fun ServiceCategoryDto.toEntity() = ServiceCategoryEntity(
    id = id,
    name = name,
    iconName = null, // or mapping iconUrl to iconName if needed
    iconUrl = ApiEndpoints.resolveImageUrl(iconUrl)
)
