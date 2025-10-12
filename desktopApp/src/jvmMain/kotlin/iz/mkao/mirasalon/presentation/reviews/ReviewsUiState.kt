package iz.mkao.mirasalon.presentation.reviews

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.AdminReview

data class ReviewsUiState(
    val reviews: List<AdminReview> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventSink: (ReviewsEvent) -> Unit = {}
) : CircuitUiState

/** Circuit UI events for the reviews admin screen. */
sealed interface ReviewsEvent : CircuitUiEvent {
    data class Search(val query: String) : ReviewsEvent
    data object Refresh : ReviewsEvent
    data class ToggleVisibility(val id: String, val isVisible: Boolean) : ReviewsEvent
    data class DeleteReview(val id: String) : ReviewsEvent
    data class Reply(val id: String, val reply: String) : ReviewsEvent
}
