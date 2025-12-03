package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.domain.model.AdminReview
import iz.mkao.mirasalon.core.network.model.dto.AdminReviewDto

fun AdminReviewDto.toDomain() = AdminReview(
    id = id,
    customerId = userId,
    customerName = userName ?: "Unknown",
    targetId = targetId,
    targetType = targetType,
    targetName = targetName,
    rating = rating,
    comment = comment,
    createdAt = createdAt,
    isVisible = isVisible,
    adminReply = adminReply
)
