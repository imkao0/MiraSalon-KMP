package iz.mkao.mirasalon.core.navigation

import androidx.navigation3.runtime.NavKey
import com.slack.circuit.runtime.screen.Screen

/**
 * Base marker interface for all routes in the application.
 * Feature modules extend this interface to define their specific screens.
 */
interface Route : NavKey, Screen, CommonParcelable
