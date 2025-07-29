package iz.mkao.mirasalon.feature.profile.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.profile.domain.model.AppTheme
import iz.mkao.mirasalon.feature.profile.domain.model.UserProfile

data class ProfileState(
    val isLoading: Boolean,
    val profile: UserProfile?,
    val addressCount: Int,
    val unreadMessagesCount: Int = 0,
    val upcomingRemindersCount: Int = 0,
    val notificationBadgeCount: Int = 0,
    val currentTheme: AppTheme = AppTheme.SYSTEM,
    val inAppNotificationsEnabled: Boolean = true,
    val error: String? = null,
    val eventSink: (ProfileEvent) -> Unit
) : CircuitUiState

sealed interface ProfileEvent : CircuitUiEvent {
    data object EditProfile : ProfileEvent
    data object SavedAddresses : ProfileEvent
    data object MyOrders : ProfileEvent
    data object Favourites : ProfileEvent
    data object PaymentMethods : ProfileEvent
    data object Notifications : ProfileEvent
    data class ToggleInAppNotifications(val enabled: Boolean) : ProfileEvent
    data object AppSettings : ProfileEvent
    data object CurrencyAndTheme : ProfileEvent
    data class SetTheme(val theme: AppTheme) : ProfileEvent
    data object Logout : ProfileEvent
    data object Retry : ProfileEvent
}
