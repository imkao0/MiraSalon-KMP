package iz.mkao.mirasalon.feature.auth.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import iz.mkao.mirasalon.feature.auth.presentation.circuit.AuthState

@Composable
fun RegisterScreen(state: AuthState, modifier: Modifier = Modifier) {
    // Both Login and Register now use the unified split-screen tabbed interface in LoginScreen.kt
    LoginScreen(state = state, modifier = modifier)
}