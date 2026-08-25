package iz.mkao.mirasalon.feature.notifications.data.repository

import iz.mkao.mirasalon.core.domain.model.Notification
import iz.mkao.mirasalon.core.domain.model.NotificationType
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.feature.notifications.data.network.api.NotificationApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationRepositoryImpl(
    private val api: NotificationApi,
    private val realtimeGateway: RealtimeGateway,
    private val repositoryScope: CoroutineScope,
    private val desktopNotifier: DesktopNotifier? = null
) : NotificationRepository {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    override val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    override val unreadCount: Flow<Int> = notifications.map { list ->
        list.count { it.isUnread }
    }

    init {
        repositoryScope.launch {
            realtimeGateway.events.collectLatest { event ->
                when (event) {
                    is DomainEvent.BookingUpdated,
                    is DomainEvent.BookingCreated,
                    is DomainEvent.NotificationReceived,
                    is DomainEvent.AppointmentReminder,
                    is DomainEvent.ChatMessageReceived,
                    is DomainEvent.PromotionChanged -> {
                        fetchNotifications()
                    }
                    else -> {}
                }
            }
        }
        repositoryScope.launch { fetchNotifications() }
    }

    override suspend fun fetchNotifications(type: NotificationType?) {
        val result = api.fetchNotifications(type?.name)
        if (result is NetworkResult.Success) {
            _notifications.value = result.data.map { dto ->
                Notification(
                    id = dto.id,
                    senderName = dto.senderName,
                    senderAvatarUrl = dto.senderAvatarUrl,
                    message = dto.message,
                    timestamp = dto.timestamp,
                    isUnread = dto.isUnread,
                    type = try {
                        NotificationType.valueOf(dto.type)
                    } catch (_: Exception) {
                        NotificationType.MESSAGE
                    },
                    thumbnail = dto.thumbnail
                )
            }
        }
    }

    override suspend fun markAsRead(id: String) {
        val result = api.markAsRead(id)
        if (result is NetworkResult.Success) {
            _notifications.update { current ->
                current.map {
                    if (it.id == id) it.copy(isUnread = false) else it
                }
            }
        }
    }

    override suspend fun clearNotifications() {
        val result = api.clearAll()
        if (result is NetworkResult.Success) {
            _notifications.value = emptyList()
        }
    }

    override suspend fun notifyChatReply(targetUserId: String, senderName: String, message: String, conversationId: String?, senderAvatar: String?) {
        desktopNotifier?.showNotification(
            title = "New Message from $senderName",
            message = message
        )
        api.notifyChatReply(targetUserId, senderName, message, conversationId ?: "", senderAvatar)
    }
}

interface DesktopNotifier {
    fun showNotification(title: String, message: String)
}

expect fun createDesktopNotifier(): DesktopNotifier?
