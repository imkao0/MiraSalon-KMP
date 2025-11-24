package iz.mkao.mirasalon.feature.appointments.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.appointments.domain.model.Appointment

data class AppointmentDetailState(
    val appointment: Appointment? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventSink: (AppointmentDetailEvent) -> Unit = {}
) : CircuitUiState

sealed interface AppointmentDetailEvent : CircuitUiEvent {
    data object Back : AppointmentDetailEvent
    data object Cancel : AppointmentDetailEvent
    data object ViewMap : AppointmentDetailEvent
    data class SpecialistClicked(val specialistId: String) : AppointmentDetailEvent
}
