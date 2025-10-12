package iz.mkao.mirasalon.presentation.settings

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.AdminSalon

data class SettingsUiState(
    val salon: AdminSalon? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventSink: (SettingsEvent) -> Unit = {}
) : CircuitUiState

/** Circuit UI events for the settings admin screen. */
sealed interface SettingsEvent : CircuitUiEvent {
    data class UpdateSalon(val salon: AdminSalon) : SettingsEvent
    data object Refresh : SettingsEvent
}
