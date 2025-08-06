package iz.mkao.mirasalon.feature.profile.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.profile.domain.model.AppSettings
import iz.mkao.mirasalon.feature.profile.domain.model.AppTheme

data class AppSettingsState(
    val settings: AppSettings,
    val eventSink: (AppSettingsEvent) -> Unit
) : CircuitUiState

sealed interface AppSettingsEvent : CircuitUiEvent {
    data class SetTheme(val theme: AppTheme) : AppSettingsEvent
    data object Back : AppSettingsEvent
}
