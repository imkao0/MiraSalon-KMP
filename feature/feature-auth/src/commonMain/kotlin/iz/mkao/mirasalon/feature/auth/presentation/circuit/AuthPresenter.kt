package iz.mkao.mirasalon.feature.auth.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.navigation.AuthRoute
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.network.model.dto.LoginRequest
import iz.mkao.mirasalon.core.network.model.dto.RegisterRequest
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.feature.auth.data.repository.AuthRepository
import kotlinx.coroutines.launch

class WelcomePresenter(
    private val navigator: Navigator
) : Presenter<AuthState> {
    @Composable
    override fun present(): AuthState {
        return AuthState(
            route = AuthRoute.Welcome,
            eventSink = { event ->
                when (event) {
                    AuthEvent.NavigateToLogin -> navigator.goTo(AuthRoute.Login)
                    AuthEvent.NavigateToRegister -> navigator.goTo(AuthRoute.Register)
                    AuthEvent.ContinueAsGuest -> navigator.resetRoot(BottomNavKey.Home())
                    else -> Unit
                }
            }
        )
    }
}

class LoginPresenter(
    private val repository: AuthRepository,
    private val navigator: Navigator
) : Presenter<AuthState> {
    @Composable
    override fun present(): AuthState {
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var isSuccess by remember { mutableStateOf(false) }
        var rememberMe by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()

        var savedEmail by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            savedEmail = repository.getSavedEmail()
        }

        return AuthState(
            route = AuthRoute.Login,
            isLoading = isLoading,
            error = error,
            isSuccess = isSuccess,
            rememberMe = rememberMe,
            savedEmail = savedEmail,
            eventSink = { event ->
                when (event) {
                    is AuthEvent.Login -> {
                        Napier.d("Login event received for ${event.email}")
                        scope.launch {
                            isLoading = true
                            error = null
                            
                            if (rememberMe) {
                                repository.saveEmail(event.email)
                            } else {
                                repository.saveEmail(null)
                            }

                            when (val result = repository.login(LoginRequest(event.email, event.password))) {
                                is NetworkResult.Success -> {
                                    Napier.d("Login successful!")
                                    isSuccess = true
                                    navigator.resetRoot(BottomNavKey.Home())
                                }
                                is NetworkResult.Error -> {
                                    Napier.e("Login failed: ${result.error.message}")
                                    isLoading = false
                                    error = result.error.message
                                }
                            }
                        }
                    }
                    AuthEvent.ToggleRememberMe -> {
                        rememberMe = !rememberMe
                    }
                    AuthEvent.NavigateToRegister -> navigator.goTo(AuthRoute.Register)
                    AuthEvent.Back -> navigator.pop()
                    AuthEvent.ForgotPassword -> { /* Handle forgot password */ }
                    AuthEvent.ClearError -> error = null
                    AuthEvent.AuthSuccess -> {
                        if (navigator.pop() == null) {
                            navigator.resetRoot(BottomNavKey.Home())
                        }
                    }
                    else -> Unit
                }
            }
        )
    }
}

class RegisterPresenter(
    private val repository: AuthRepository,
    private val navigator: Navigator
) : Presenter<AuthState> {
    @Composable
    override fun present(): AuthState {
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var isSuccess by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        return AuthState(
            route = AuthRoute.Register,
            isLoading = isLoading,
            error = error,
            isSuccess = isSuccess,
            eventSink = { event ->
                when (event) {
                    is AuthEvent.Register -> {
                        Napier.d("Register event received for ${event.email}")
                        scope.launch {
                            isLoading = true
                            error = null
                            when (val result = repository.register(RegisterRequest(event.name, event.email, event.password))) {
                                is NetworkResult.Success -> {
                                    Napier.d("Register successful!")
                                    isSuccess = true
                                    navigator.resetRoot(BottomNavKey.Home())
                                }
                                is NetworkResult.Error -> {
                                    Napier.e("Register failed: ${result.error.message}")
                                    isLoading = false
                                    error = result.error.message
                                }
                            }
                        }
                    }
                    AuthEvent.NavigateToLogin -> navigator.goTo(AuthRoute.Login)
                    AuthEvent.Back -> navigator.pop()
                    AuthEvent.ClearError -> error = null
                    AuthEvent.AuthSuccess -> {
                        if (navigator.pop() == null) {
                            navigator.resetRoot(BottomNavKey.Home())
                        }
                    }
                    else -> Unit
                }
            }
        )
    }
}

class AuthManualPresenterFactory(
    private val repository: AuthRepository
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is AuthRoute.Welcome -> WelcomePresenter(navigator)
            is AuthRoute.Login -> LoginPresenter(repository, navigator)
            is AuthRoute.Register -> RegisterPresenter(repository, navigator)
            else -> null
        }
    }
}