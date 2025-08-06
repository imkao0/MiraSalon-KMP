package iz.mkao.mirasalon.feature.profile.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.profile.domain.model.NotificationPreferences

data class NotificationPreferencesState(
    val preferences: NotificationPreferences,
    val eventSink: (NotificationPreferencesEvent) -> Unit
) : CircuitUiState

sealed interface NotificationPreferencesEvent : CircuitUiEvent {
    data class ToggleInApp(val enabled: Boolean) : NotificationPreferencesEvent
    data class TogglePush(val enabled: Boolean) : NotificationPreferencesEvent
    data class ToggleSpecialistMessages(val enabled: Boolean) : NotificationPreferencesEvent
    data class ToggleBookingReminders(val enabled: Boolean) : NotificationPreferencesEvent
    data class ToggleMarketing(val enabled: Boolean) : NotificationPreferencesEvent
    data object Back : NotificationPreferencesEvent
}
