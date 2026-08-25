package iz.mkao.mirasalon.data.repository

import iz.mkao.mirasalon.core.domain.model.Notification
import iz.mkao.mirasalon.core.domain.model.NotificationType
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.feature.notifications.data.repository.DesktopNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

class DesktopNotificationRepository(
    private val notifier: DesktopNotifier?
) : NotificationRepository {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    override val notifications: Flow<List<Notification>> = _notifications.asStateFlow()
    override val unreadCount: Flow<Int> = notifications.map { list -> list.count { it.isUnread } }

    override suspend fun notifyChatReply(
        targetUserId: String,
        senderName: String,
        message: String,
        conversationId: String?,
        senderAvatar: String?
    ) {

        val newNotification = Notification(
            id = UUID.randomUUID().toString(),
            senderName = senderName,
            senderAvatarUrl = senderAvatar,
            message = message,
            timestamp = System.currentTimeMillis(),
            isUnread = true,
            type = NotificationType.MESSAGE
        )
        _notifications.update { it + newNotification }


        notifier?.showNotification("New Message from $senderName", message)
    }

    override suspend fun clearNotifications() {
        _notifications.value = emptyList()
    }

    override suspend fun fetchNotifications(type: NotificationType?) {

    }

    override suspend fun markAsRead(id: String) {
        _notifications.update { current ->
            current.map { if (it.id == id) it.copy(isUnread = false) else it }
        }
    }
}
