package iz.mkao.mirasalon.presentation

import androidx.compose.runtime.compositionLocalOf
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.navigation.CommonParcelable
import iz.mkao.mirasalon.core.navigation.CommonParcelize

val LocalDesktopNavigate = compositionLocalOf<(String) -> Unit> { { _ -> } }
val LocalSidebarExpanded = compositionLocalOf { true }
val LocalToggleSidebar = compositionLocalOf { { } }
val LocalProfileClick = compositionLocalOf { { } }

/**
 * Circuit [Screen]s for the desktop admin app.
 *
 * The desktop shell keeps its own simple string-based navigation, so these
 * screens act purely as Circuit keys binding each presenter to its UI.
 */
sealed interface DesktopScreen : Screen, CommonParcelable {
    @CommonParcelize
    data object Dashboard : DesktopScreen

    @CommonParcelize
    data object Analytics : DesktopScreen

    @CommonParcelize
    data object Bookings : DesktopScreen

    @CommonParcelize
    data object Calendar : DesktopScreen

    @CommonParcelize
    data class Chat(val sessionId: String? = null) : DesktopScreen

    @CommonParcelize
    data object Customers : DesktopScreen

    @CommonParcelize
    data object Orders : DesktopScreen

    @CommonParcelize
    data object Products : DesktopScreen

    @CommonParcelize
    data object Promotions : DesktopScreen

    @CommonParcelize
    data object Reviews : DesktopScreen

    @CommonParcelize
    data object Services : DesktopScreen

    @CommonParcelize
    data object Settings : DesktopScreen

    @CommonParcelize
    data object Staff : DesktopScreen

    @CommonParcelize
    data object Help : DesktopScreen
}
