package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    val senderName: String,
    val senderAvatarUrl: String?,
    val message: String,
    val timestamp: Long,
    val isUnread: Boolean,
    val type: NotificationType,
    val thumbnail: String? = null
)

@Serializable
enum class NotificationType {
   COMMENT, PROMO, MESSAGE, REMINDER
}