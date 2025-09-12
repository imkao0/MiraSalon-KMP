package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.domain.model.AdminReview
import iz.mkao.mirasalon.core.domain.model.Review
import iz.mkao.mirasalon.core.network.model.dto.AdminReviewDto
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto

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

fun ReviewDto.toDomain() = Review(
    id = id.ifBlank { reviewId },
    userName = userName.ifBlank { customerName },
    userAvatarUrl = userAvatarUrl ?: customerAvatarUrl,
    rating = rating,
    comment = comment,
    createdAtEpochSeconds = if (createdAtEpochSeconds > 0) createdAtEpochSeconds else createdAt / 1000
)