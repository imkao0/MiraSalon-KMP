package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.network.model.dto.ServiceCategoryDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import iz.mkao.mirasalon.core.network.config.ApiEndpoints

fun ServiceDto.toDomain(): Service = Service(
    id = id,
    name = name,
    description = description ?: "",
    price = price,
    durationMinutes = durationMinutes,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    categoryId = categoryId ?: "",
    subCategory = subCategory,
    discountPercent = discountPercent,
    rating = rating
)

fun ServiceCategoryDto.toDomain(): ServiceCategory = ServiceCategory(
    id = id,
    name = name,
    iconUrl = ApiEndpoints.resolveImageUrl(iconUrl)
)
