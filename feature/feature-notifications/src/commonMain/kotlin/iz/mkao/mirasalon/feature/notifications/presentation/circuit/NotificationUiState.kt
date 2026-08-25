package iz.mkao.mirasalon.feature.notifications.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.NotificationType

data class NotificationState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false,
    val currentTimeMillis: Long = 0L,
    val selectedFilter: NotificationType? = null,
    val eventSink: (NotificationEvent) -> Unit = {}
) : CircuitUiState

sealed interface NotificationEvent : CircuitUiEvent {
    data object BackClicked : NotificationEvent
    data class NotificationClicked(val id: String) : NotificationEvent
    data class FilterChanged(val type: NotificationType?) : NotificationEvent
    data object ClearAll : NotificationEvent
}

data class NotificationItem(
    val id: String,
    val senderName: String,
    val senderAvatarUrl: String?,
    val message: String,
    val time: String,
    val isUnread: Boolean,
    val timestamp: Long,
    val type: NotificationType,
    val thumbnail: String? = null,
    val reminderTimeDetails: String? = null,
)
