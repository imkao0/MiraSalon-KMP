package iz.mkao.mirasalon.core.network.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val message: String,
    val timestamp: Long,
    val isUnread: Boolean,
    val type: String,
    val thumbnail: String? = null
)