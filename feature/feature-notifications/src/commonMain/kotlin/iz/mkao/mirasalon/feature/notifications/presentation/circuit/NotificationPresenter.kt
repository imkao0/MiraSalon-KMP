package iz.mkao.mirasalon.feature.notifications.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.core.domain.model.Notification
import iz.mkao.mirasalon.core.domain.model.NotificationType
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.core.navigation.NotificationRoute
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class NotificationPresenter(
    private val repository: NotificationRepository,
    private val navigator: Navigator,
) : Presenter<NotificationState> {

    @Composable
    override fun present(): NotificationState {
        val notifications by repository.notifications.collectAsState(emptyList())
        var selectedFilter by remember { mutableStateOf<NotificationType?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(selectedFilter) {
            repository.fetchNotifications(selectedFilter)
        }

        return NotificationState(
            notifications = notifications.map { it.toUiModel() },
            selectedFilter = selectedFilter,
            eventSink = { event ->
                when (event) {
                    NotificationEvent.BackClicked -> navigator.pop()
                    is NotificationEvent.NotificationClicked -> {
                        scope.launch { repository.markAsRead(event.id) }
                    }
                    is NotificationEvent.FilterChanged -> {
                        selectedFilter = event.type
                    }
                    NotificationEvent.ClearAll -> {
                        scope.launch { repository.clearNotifications() }
                    }
                }
            }
        )
    }

    private fun Notification.toUiModel(): NotificationItem {
        val meaningfulMessage = if (message.contains("Mira salon you have a new notification", ignoreCase = true) || 
            message.contains("You have a new notification", ignoreCase = true)) {
            when (type) {
                NotificationType.MESSAGE -> "sent you a message"
                NotificationType.PROMO -> "shared a new promotion with you"
                NotificationType.REMINDER -> "Reminder: Your appointment is coming up"
                NotificationType.COMMENT -> "commented on your post"
            }
        } else message

        return NotificationItem(
            id = id,
            senderName = senderName,
            senderAvatarUrl = senderAvatarUrl,
            message = meaningfulMessage,
            time = formatTime(timestamp),
            isUnread = isUnread,
            type = type,
            thumbnail = thumbnail,
            reminderTimeDetails = if (type == NotificationType.REMINDER) {
                // Try extracting from original message first, then meaningful one
                extractReminderTimeDetails(message, timestamp) ?: extractReminderTimeDetails(meaningfulMessage, timestamp)
            } else null
        )
    }

    private fun extractReminderTimeDetails(message: String, timestamp: Long): String? {
        val regex = Regex("in (\\d+)\\s*(minutes?|hours?|days?)")
        val match = regex.find(message) ?: return null
        val (valueStr, unit) = match.destructured
        val value = valueStr.toLongOrNull() ?: return null

        val duration = when {
            unit.startsWith("min") -> value.minutes
            unit.startsWith("hour") -> value.hours
            unit.startsWith("day") -> value.days
            else -> return null
        }

        val eventTime = Instant.fromEpochMilliseconds(timestamp) + duration
        val formattedTime = DateUtils.formatTime(eventTime.epochSeconds)

        return when {
            unit.startsWith("min") -> "$value min before ($formattedTime)"
            unit.startsWith("hour") -> "$value hr before ($formattedTime)"
            unit.startsWith("day") -> {
                val formattedDate = DateUtils.formatDateSeparator(eventTime.epochSeconds)
                "$value day${if (value > 1) "s" else ""} before ($formattedDate)"
            }
            else -> null
        }
    }

    private fun formatTime(timestamp: Long): String {
        val currentMillis = Clock.System.now().toEpochMilliseconds()
        return DateUtils.formatRelativeTime(timestamp, currentMillis)
    }
}

class NotificationPresenterFactory(
    private val repository: NotificationRepository
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is NotificationRoute.Notifications -> NotificationPresenter(repository, navigator)
            else -> null
        }
    }
}
