package iz.mkao.mirasalon.feature.chat.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChatItem(
    val id: String,
    val contactName: String,
    val contactRole: String? = null,
    val avatarUrl: String?,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.Sent,
)

enum class DeliveryStatus { Sent, Delivered, Read }
