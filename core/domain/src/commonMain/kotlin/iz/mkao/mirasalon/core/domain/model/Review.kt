package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

/** A customer review as shown in the admin moderation screen. */
@Serializable
data class AdminReview(
    val id: String,
    val customerId: String = "",
    val customerName: String = "",
    val targetId: String = "",
    val targetType: String = "",
    val targetName: String? = null,
    val rating: Int = 0,
    val comment: String? = null,
    val createdAt: Long = 0L,
    val isVisible: Boolean = true,
    val adminReply: String? = null
)

@Serializable
data class Review(
    val id: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val rating: Int,
    val comment: String? = null,
    val createdAtEpochSeconds: Long,
)

@Serializable
data class SpecialistReview(
    val id: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val rating: Int,
    val comment: String? = null,
    val createdAtEpochSeconds: Long,
)
