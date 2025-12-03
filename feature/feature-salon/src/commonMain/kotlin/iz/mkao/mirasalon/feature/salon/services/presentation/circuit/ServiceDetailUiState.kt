package iz.mkao.mirasalon.feature.salon.services.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.Specialist

data class ServiceDetailState(
    val isLoading: Boolean = false,
    val service: Service? = null,
    val categoryName: String? = null,
    val categoryIconKey: String? = null,
    val isFavorited: Boolean = false,
    val isBookmarked: Boolean = false,
    val specialists: List<Specialist> = emptyList(),
    val relatedServices: List<Service> = emptyList(),
    val error: String? = null,
    val unreadNotificationCount: Int = 0,
    val onReviewSubmit: (suspend (Int, String) -> Result<Unit>)? = null,
    val eventSink: (ServiceDetailEvent) -> Unit
) : CircuitUiState

sealed interface ServiceDetailEvent : CircuitUiEvent {
    data object BackClicked : ServiceDetailEvent
    data object Retry : ServiceDetailEvent
    data object BookClicked : ServiceDetailEvent
    data object SaveClicked : ServiceDetailEvent
    data object ToggleFavorite : ServiceDetailEvent
    data object ToggleBookmark : ServiceDetailEvent
    data object NotificationClicked : ServiceDetailEvent
    data class SpecialistClicked(val specialistId: String) : ServiceDetailEvent
    data class RelatedServiceClicked(val serviceId: String) : ServiceDetailEvent
}
