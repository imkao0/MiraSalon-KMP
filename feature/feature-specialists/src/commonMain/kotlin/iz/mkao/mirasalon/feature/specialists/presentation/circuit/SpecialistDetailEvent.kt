package iz.mkao.mirasalon.feature.specialists.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import iz.mkao.mirasalon.core.domain.model.Specialist

sealed interface SpecialistDetailEvent : CircuitUiEvent {
    data object Back : SpecialistDetailEvent
    data class ChatClicked(val specialist: Specialist) : SpecialistDetailEvent
    data class BookServiceClicked(val serviceId: String) : SpecialistDetailEvent
    data object BookAppointmentClicked : SpecialistDetailEvent
    data object SaveClicked : SpecialistDetailEvent
    data object WriteReviewClicked : SpecialistDetailEvent
    data object DismissReviewSheet : SpecialistDetailEvent
}
