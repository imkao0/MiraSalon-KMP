package iz.mkao.mirasalon.core.network.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminReviewDto(
    val id: String,
    val userId: String,
    val userName: String? = null,
    val targetId: String,
    val targetType: String,
    val targetName: String,
    val rating: Int,
    val comment: String? = null,
    val adminReply: String? = null,
    val createdAt: Long,
    val isVisible: Boolean = true
)

@Serializable
data class AdminReplyRequest(
    val reply: String
)

@Serializable
data class UpdateReviewVisibilityRequest(
    val isVisible: Boolean
)
