package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.Notification
import iz.mkao.mirasalon.core.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface NotificationRepository {
    suspend fun notifyChatReply(targetUserId: String, senderName: String, message: String, conversationId: String? = null, senderAvatar: String? = null)
    val notifications: Flow<List<Notification>>
    val unreadCount: Flow<Int>
    suspend fun clearNotifications()
    suspend fun fetchNotifications(type: NotificationType? = null)
    suspend fun markAsRead(id: String)
}
