package iz.mkao.mirasalon.data.repository

import io.getstream.chat.java.models.Channel
import io.getstream.chat.java.models.Message
import io.getstream.chat.java.models.Sort
import io.getstream.chat.java.models.User
import io.getstream.chat.java.services.framework.DefaultClient
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.model.Notification
import iz.mkao.mirasalon.core.domain.model.NotificationType
import iz.mkao.mirasalon.core.domain.model.chat.ChatMessage
import iz.mkao.mirasalon.core.domain.model.chat.ChatSession
import iz.mkao.mirasalon.core.domain.model.chat.MessageContent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.core.domain.repository.StreamChatManager
import iz.mkao.mirasalon.feature.notifications.data.repository.DesktopNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Properties
import iz.mkao.mirasalon.core.realtime.DesktopStreamChatManager

class DesktopNotificationRepository(
    private val notifier: DesktopNotifier?
) : NotificationRepository {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    override val notifications: Flow<List<Notification>> = _notifications.asStateFlow()
    override val unreadCount: Flow<Int> = notifications.map { list -> list.count { it.isUnread } }

    override suspend fun notifyChatReply(targetUserId: String, senderName: String, conversationId: String?) {

        val newNotification = Notification(
            id = java.util.UUID.randomUUID().toString(),
            senderName = senderName,
            senderAvatarUrl = null,
            message = "replied to your message",
            timestamp = System.currentTimeMillis(),
            isUnread = true,
            type = NotificationType.MESSAGE
        )
        _notifications.update { it + newNotification }


        notifier?.showNotification("New Message from $senderName", "replied to your message")
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
