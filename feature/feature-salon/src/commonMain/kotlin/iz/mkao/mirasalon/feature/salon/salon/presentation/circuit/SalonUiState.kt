package iz.mkao.mirasalon.feature.salon.salon.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.model.SalonCategory
import iz.mkao.mirasalon.core.domain.model.Specialist

/**
 * Flat, immutable Circuit state for the salon home screen. All data is owned
 * by [SalonPresenter]; the UI is a pure function of this state and reports
 * user intents exclusively through [eventSink].
 */
data class SalonState(
    val userName: String? = null,
    val userLocation: String? = null,
    val userAvatarUrl: String? = null,
    val categories: List<SalonCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val specialists: List<Specialist> = emptyList(),
    val promotions: List<Promotion> = emptyList(),
    val usedPromotionIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Non-null when the user tapped a promotion; the UI copies it and consumes it. */
    val promotionCodeToCopy: String? = null,
    /** Unread notification count (messages, reminders, offers) when in-app notifications are enabled */
    val unreadNotificationCount: Int = 0,
    /** Whether in-app notifications are enabled from profile settings */
    val inAppNotificationsEnabled: Boolean = true,
    val eventSink: (SalonEvent) -> Unit = {}
) : CircuitUiState

sealed interface SalonEvent : CircuitUiEvent {
    data object NotificationClicked : SalonEvent
    data object FavoriteClicked : SalonEvent
    data class CategorySelected(val id: String) : SalonEvent
    data object ViewAllCategories : SalonEvent
    data class SpecialistSelected(val id: String) : SalonEvent
    data object ViewAllSpecialists : SalonEvent
    data class PromotionClicked(val id: String) : SalonEvent
    data class SearchQueryChanged(val query: String) : SalonEvent
    /** The UI has copied/consumed [SalonState.promotionCodeToCopy]. */
    data object PromotionCodeConsumed : SalonEvent
    data object Retry : SalonEvent
}
