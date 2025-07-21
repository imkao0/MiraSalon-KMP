package iz.mkao.mirasalon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraSalonTheme
import iz.mkao.mirasalon.core.navigation.AuthRoute
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.data.local.TokenManager
import iz.mkao.mirasalon.data.remote.AuthClient
import iz.mkao.mirasalon.data.remote.DesktopNotificationService
import iz.mkao.mirasalon.di.initKoin
import iz.mkao.mirasalon.presentation.DesktopNoOpNavigator
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.LocalDesktopNavigate
import iz.mkao.mirasalon.presentation.LocalProfileClick
import iz.mkao.mirasalon.presentation.LocalSidebarExpanded
import iz.mkao.mirasalon.presentation.LocalToggleSidebar
import iz.mkao.mirasalon.presentation.profile.ProfileDialog
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext

fun main() {
    Napier.base(DebugAntilog())
    
    // Configure API base URL for desktop app
    ApiEndpoints.setBaseUrl("http://127.0.0.1:8080")
    
    initKoin()

    @OptIn(ExperimentalCoilApi::class)
    SingletonImageLoader.setSafe {
        val httpClient: HttpClient = GlobalContext.get().get()
        ImageLoader.Builder(it)
            .components {
                add(KtorNetworkFetcherFactory(httpClient))
            }
            .build()
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "MiraSalon-KMP Desktop",
        ) {
            KoinContext {
                MiraSalonTheme {
                    var currentScreen by remember { mutableStateOf<DesktopScreen>(DesktopScreen.Dashboard) }
                    var isSidebarExpanded by remember { mutableStateOf(true) }
                    var showLogoutDialog by remember { mutableStateOf(false) }
                    var showProfileDialog by remember { mutableStateOf(false) }

                    val tokenManager: TokenManager = koinInject()
                    val session by tokenManager.session.collectAsState()
                    val token = session.token

                    val snackbarHostState = remember { SnackbarHostState() }
                    val notificationService: DesktopNotificationService = koinInject()
                    val authClient: AuthClient = koinInject()

                    val circuit: Circuit = koinInject()

                    var authScreenRoute by remember { mutableStateOf<AuthRoute>(AuthRoute.Login) }
                    val authNavigator = remember {
                        object : Navigator {
                            override fun goTo(screen: Screen): Boolean {
                                if (screen is AuthRoute) {
                                    authScreenRoute = screen
                                    return true
                                }
                                return false
                            }

                            override fun pop(result: PopResult?): Screen? = null
                            override fun peek(): Screen? = authScreenRoute
                            override fun resetRoot(
                                newRoot: Screen,
                                options: Navigator.StateOptions
                            ): List<Screen> {
                                if (newRoot is AuthRoute) {
                                    authScreenRoute = newRoot
                                }
                                return listOf(authScreenRoute)
                            }

                            override fun forward(): Boolean = false
                            override fun backward(): Boolean = false
                            override fun peekBackStack(): List<Screen> = listOf(authScreenRoute)
                            override fun peekNavStack(): NavStackList<Screen>? = null
                        }
                    }

                    val handleNavigation: (String) -> Unit = { route ->
                        when (route) {
                            "Sign out" -> showLogoutDialog = true
                            "Dashboard" -> currentScreen = DesktopScreen.Dashboard
                            "Analytics" -> currentScreen = DesktopScreen.Analytics
                            "Bookings" -> currentScreen = DesktopScreen.Bookings
                            "Calendar" -> currentScreen = DesktopScreen.Calendar
                            "Chat" -> currentScreen = DesktopScreen.Chat()
                            "Customers" -> currentScreen = DesktopScreen.Customers
                            "Orders" -> currentScreen = DesktopScreen.Orders
                            "Products" -> currentScreen = DesktopScreen.Products
                            "Promotions" -> currentScreen = DesktopScreen.Promotions
                            "Reviews" -> currentScreen = DesktopScreen.Reviews
                            "Services" -> currentScreen = DesktopScreen.Services
                            "Settings" -> currentScreen = DesktopScreen.Settings
                            "Staff" -> currentScreen = DesktopScreen.Staff
                            "Help" -> currentScreen = DesktopScreen.Help
                        }
                    }

                    LaunchedEffect(Unit) {
                        notificationService.notifications.collectLatest { message ->
                            snackbarHostState.showSnackbar(message)
                        }
                    }

                    if (showLogoutDialog) {
                        AlertDialog(
                            onDismissRequest = { showLogoutDialog = false },
                            title = { Text("Confirm Logout") },
                            text = { Text("Are you sure you want to sign out from the system?") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        tokenManager.clearToken()
                                        showLogoutDialog = false
                                        currentScreen = DesktopScreen.Dashboard
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiraCoral)
                                ) {
                                    Text("Sign Out")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLogoutDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (showProfileDialog) {
                        ProfileDialog(
                            tokenManager = tokenManager,
                            authClient = authClient,
                            uploadRepository = koinInject(),
                            onDismiss = { showProfileDialog = false }
                        )
                    }

                    CircuitCompositionLocals(circuit) {
                        CompositionLocalProvider(
                            LocalDesktopNavigate provides handleNavigation,
                            LocalSidebarExpanded provides isSidebarExpanded,
                            LocalToggleSidebar provides { isSidebarExpanded = !isSidebarExpanded },
                            LocalProfileClick provides { showProfileDialog = true }
                        ) {
                            Scaffold(
                                snackbarHost = { SnackbarHost(snackbarHostState) }
                            ) { padding ->
                                Box(modifier = Modifier.padding(padding)) {
                                    if (token == null) {
                                        CircuitContent(
                                            screen = authScreenRoute,
                                            navigator = authNavigator
                                        )
                                    } else {
                                        CircuitContent(
                                            screen = currentScreen,
                                            navigator = DesktopNoOpNavigator
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
