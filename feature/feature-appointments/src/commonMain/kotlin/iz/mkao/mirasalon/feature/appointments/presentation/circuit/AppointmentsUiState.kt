package iz.mkao.mirasalon.feature.appointments.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.appointments.domain.model.Appointment

data class AppointmentsState(
    val isLoading: Boolean = true,
    val groupedAppointments: Map<String, List<Appointment>> = emptyMap(),
    val error: String? = null,
    val eventSink: (AppointmentsEvent) -> Unit = {},
) : CircuitUiState

sealed interface AppointmentsEvent : CircuitUiEvent {
    data object Refresh : AppointmentsEvent
    data object Back : AppointmentsEvent
    data class AppointmentClicked(val id: String) : AppointmentsEvent
    data class SpecialistClicked(val id: String) : AppointmentsEvent
    data class CancelAppointment(val id: String) : AppointmentsEvent
}
