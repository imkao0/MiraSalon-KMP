package iz.mkao.mirasalon.core.navigation

import androidx.compose.runtime.staticCompositionLocalOf

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No Navigator provided")
}
