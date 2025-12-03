package iz.mkao.mirasalon.feature.auth.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.navigation.AuthRoute

data class AuthState(
    val route: AuthRoute,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val rememberMe: Boolean = false,
    val savedEmail: String? = null,
    val eventSink: (AuthEvent) -> Unit
) : CircuitUiState

sealed interface AuthEvent : CircuitUiEvent {
    data class Login(val email: String, val password: String) : AuthEvent
    data class Register(val name: String, val email: String, val password: String) : AuthEvent
    data object ClearError : AuthEvent
    data object ToggleRememberMe : AuthEvent
    data object NavigateToLogin : AuthEvent
    data object NavigateToRegister : AuthEvent
    data object AuthSuccess : AuthEvent
    data object Back : AuthEvent
    data object ContinueAsGuest : AuthEvent
    data object ForgotPassword : AuthEvent
}
