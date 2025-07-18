package iz.mkao.mirasalon.core.navigation

import androidx.compose.runtime.State
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

interface Navigator {
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
    val currentTab: State<NavKey>
    val startRoute: NavKey

    fun navigateTo(key: NavKey): Boolean
    fun replace(key: NavKey): Boolean
    fun clearAndNavigate(key: NavKey): Boolean
    fun pop(): Boolean
    fun popUntil(predicate: (NavKey) -> Boolean)
}
