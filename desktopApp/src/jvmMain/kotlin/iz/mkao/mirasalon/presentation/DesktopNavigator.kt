package iz.mkao.mirasalon.presentation

import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * No-op [Navigator] for the desktop admin shell.
 *
 * Desktop screens are switched by the shell's own route state, so Circuit
 * navigation commands are intentionally ignored.
 */
object DesktopNoOpNavigator : Navigator {
    override fun goTo(screen: Screen): Boolean = false

    override fun pop(result: PopResult?): Screen? = null

    override fun peek(): Screen? = null

    override fun resetRoot(newRoot: Screen, options: Navigator.StateOptions): List<Screen> =
        emptyList()

    override fun forward(): Boolean = false

    override fun backward(): Boolean = false

    override fun peekBackStack(): List<Screen> = emptyList()

    override fun peekNavStack(): NavStackList<Screen>? = null
}
