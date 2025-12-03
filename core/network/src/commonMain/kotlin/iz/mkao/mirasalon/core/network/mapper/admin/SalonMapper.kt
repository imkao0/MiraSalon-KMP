package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.domain.model.AdminSalon
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.SalonDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateSalonRequest

fun SalonDto.toDomain() = AdminSalon(
    id = id,
    name = name,
    address = address,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    phone = phone,
    rating = rating,
    openTime = openTime,
    closeTime = closeTime,
    timezoneId = timezoneId
)

fun AdminSalon.toUpdateRequest() = UpdateSalonRequest(
    name = name,
    address = address,
    imageUrl = imageUrl,
    phone = phone,
    openTime = openTime,
    closeTime = closeTime,
    timezoneId = timezoneId
)
