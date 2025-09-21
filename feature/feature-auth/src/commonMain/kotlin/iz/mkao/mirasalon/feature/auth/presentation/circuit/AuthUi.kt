package iz.mkao.mirasalon.feature.auth.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.navigation.AuthRoute
import iz.mkao.mirasalon.feature.auth.presentation.ui.LoginScreen
import iz.mkao.mirasalon.feature.auth.presentation.ui.RegisterScreen
import iz.mkao.mirasalon.feature.auth.presentation.ui.WelcomeScreen

@Composable
fun WelcomeUi(state: AuthState, modifier: Modifier = Modifier) {
    AuthUiInternal(state, modifier) { WelcomeScreen(it, modifier) }
}

@Composable
fun LoginUi(state: AuthState, modifier: Modifier = Modifier) {
    AuthUiInternal(state, modifier) { LoginScreen(it, modifier) }
}

@Composable
fun RegisterUi(state: AuthState, modifier: Modifier = Modifier) {
    AuthUiInternal(state, modifier) { RegisterScreen(it, modifier) }
}

@Composable
private fun AuthUiInternal(
    state: AuthState,
    modifier: Modifier = Modifier,
    content: @Composable (AuthState) -> Unit
) {
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            state.eventSink(AuthEvent.AuthSuccess)
        }
    }

    content(state)
}

class AuthManualUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is AuthRoute.Welcome -> ui<AuthState> { state, modifier -> WelcomeUi(state, modifier) }
            is AuthRoute.Login -> ui<AuthState> { state, modifier -> LoginUi(state, modifier) }
            is AuthRoute.Register -> ui<AuthState> { state, modifier -> RegisterUi(state, modifier) }
            else -> null
        }
    }
}