package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.domain.model.CustomerDetail
import iz.mkao.mirasalon.core.domain.model.CustomerSummary
import iz.mkao.mirasalon.core.network.model.dto.CustomerDetailDto
import iz.mkao.mirasalon.core.network.model.dto.CustomerSummaryDto
import iz.mkao.mirasalon.core.network.config.ApiEndpoints

fun CustomerSummaryDto.toDomain() = CustomerSummary(
    id = id,
    name = name,
    email = email,
    phone = phone ?: "",
    imageUrl = ApiEndpoints.resolveImageUrl(avatarUrl),
    totalBookings = totalAppointments,
    totalSpent = totalSpend,
    lastVisit = null // Not provided in summary DTO
)

fun CustomerDetailDto.toDomain() = CustomerDetail(
    id = id,
    name = name,
    email = email,
    phone = phone ?: "",
    imageUrl = ApiEndpoints.resolveImageUrl(avatarUrl),
    totalBookings = recentAppointments.size,
    totalSpent = recentOrders.sumOf { it.amount },
    createdAt = joinedAt,
    address = "", // DTO doesn't have it
    gender = "" // DTO doesn't have it
)
