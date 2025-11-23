package iz.mkao.mirasalon.feature.specialists.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent

sealed interface SpecialistsEvent : CircuitUiEvent {
    data class SpecialistClicked(val specialistId: String) : SpecialistsEvent
    data class BookSpecialistClicked(val specialistId: String) : SpecialistsEvent
    data object Back : SpecialistsEvent
    data object Retry : SpecialistsEvent
    data object Refresh : SpecialistsEvent
    data class SearchQueryChanged(val query: String) : SpecialistsEvent
}
