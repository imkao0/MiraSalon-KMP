package iz.mkao.mirasalon.feature.specialists.data.mapper

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto

fun ServiceDto.toDomain(): Service = Service(
    id = id,
    name = name,
    description = "",
    durationMinutes = durationMinutes,
    price = price,
    discountPercent = discountPercent,
    categoryId = "",
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl) ?: ApiEndpoints.getServicePlaceholder(name)
)
