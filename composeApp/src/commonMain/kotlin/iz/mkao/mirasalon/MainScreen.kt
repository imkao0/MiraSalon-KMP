package iz.mkao.mirasalon

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import iz.mkao.mirasalon.core.common.util.SnackbarController
import iz.mkao.mirasalon.core.designsystem.components.BottomNavItem
import iz.mkao.mirasalon.core.designsystem.components.MiraSnackbar
import iz.mkao.mirasalon.core.designsystem.components.SnapBottomBar
import iz.mkao.mirasalon.core.domain.model.Cart
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.repository.CartRepository
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.core.domain.repository.UpcomingAppointmentsSource
import iz.mkao.mirasalon.core.navigation.AuthRoute
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject

@Composable
fun MainScreen() {
    val realtimeGateway: RealtimeGateway = koinInject()
    val cartRepository: CartRepository = koinInject()
    val chatRepository: ChatRepository = koinInject()
    val notificationRepository: NotificationRepository = koinInject()
    val circuit: Circuit = koinInject()
    val tokenProvider: SalonTokenProvider = koinInject()

    val cart by cartRepository.observeCart()
        .collectAsStateWithLifecycle(initialValue = Cart())
        
    val conversations by chatRepository.observeConversations()
        .collectAsStateWithLifecycle(initialValue = emptyList())
        
    val unreadMessageCount = remember(conversations) {
        conversations.sumOf { it.unreadCount }
    }
    
    val unreadNotificationCount by notificationRepository.unreadCount
        .collectAsStateWithLifecycle(initialValue = 0)

    val upcomingAppointmentsSource: UpcomingAppointmentsSource = koinInject()
    val upcomingAppointmentsCount by upcomingAppointmentsSource.observeUpcomingAppointmentsCount()
        .collectAsStateWithLifecycle(initialValue = 0)

    val connectionScope = rememberCoroutineScope()
    DisposableEffect(realtimeGateway) {
        connectionScope.launch { realtimeGateway.connect() }
        onDispose {
            connectionScope.launch { realtimeGateway.disconnect() }
        }
    }

    LaunchedEffect(realtimeGateway) {
        realtimeGateway.events
            .onEach { event ->
                val message = when (event) {
                    is DomainEvent.BookingCreated -> event.message
                    is DomainEvent.BookingUpdated -> event.message
                    is DomainEvent.OrderCreated -> event.message
                    is DomainEvent.ChatMessageReceived -> null
                    is DomainEvent.AppointmentReminder -> {
                        val sender = event.specialistName?.let { "$it: " } ?: ""
                        "$sender${event.message}"
                    }
                    is DomainEvent.PromotionChanged -> event.message
                    is DomainEvent.NotificationReceived -> {
                        val sender = event.senderName?.takeIf { it.isNotBlank() }?.let { "$it: " } ?: ""
                        "$sender${event.message}"
                    }
                    // ReviewSubmitted events are routed to the admin desktop
                    // dashboard only; clients should never be notified about
                    // their own (or others') review submissions.
                    is DomainEvent.ReviewSubmitted -> null
                    is DomainEvent.UserProfileUpdated -> event.message
                    else -> null
                }
                
                message?.let { msg ->
                    SnackbarController.showSnackbar(
                        message = msg,
                    )
                }
            }
            .launchIn(this)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        SnackbarController.events.collect { event ->
          snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.action?.label,
                duration = SnackbarDuration.Short
            )
        }
    }

    val initialScreen = remember {
        runBlocking {
            if (tokenProvider.accessToken() != null) BottomNavKey.Home()
            else AuthRoute.Welcome
        }
    }
    val backStack = rememberSaveableBackStack(initialScreen)
    val navigator = rememberCircuitNavigator(backStack) { /* onRootPop */ }

    val bottomNavItems = remember(cart.itemCount, unreadMessageCount, upcomingAppointmentsCount) {
        BottomNavKey.items.map { key: BottomNavKey ->
            BottomNavItem(
                label = key.label,
                icon = key.icon,
                id = key::class,
                badgeCount = when (key) {
                    is BottomNavKey.Cart -> cart.itemCount
                    is BottomNavKey.Chat -> unreadMessageCount
                    is BottomNavKey.Booking -> upcomingAppointmentsCount
                    else -> 0
                }
            )
        }
    }

    val scope = rememberCoroutineScope()
    val onBottomNavClick: (Any) -> Unit = remember(
        navigator,
        tokenProvider,
        scope
    ) {
        { classId ->
            val key = BottomNavKey.items.find { it::class == classId }
            if (key != null) {
                if (key is BottomNavKey.Booking ||
                    key is BottomNavKey.Chat ||
                    key is BottomNavKey.Profile
                ) {
                    scope.launch {
                        if (tokenProvider.accessToken() != null) {
                            navigator.resetRoot(key)
                        } else {
                            navigator.resetRoot(AuthRoute.Welcome)
                        }
                    }
                } else {
                    navigator.resetRoot(key)
                }
            }
        }
    }

    val showBottomBar by remember(backStack) {
        derivedStateOf {
            val records = backStack.toList()
            records.size == 1 && records.lastOrNull()?.screen is BottomNavKey
        }
    }
    val currentScreen by remember(backStack) {
        derivedStateOf { backStack.toList().lastOrNull()?.screen }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar && currentScreen != null) {
                SnapBottomBar(
                    items = bottomNavItems,
                    selectedItemId = currentScreen!!::class,
                    onItemClick = onBottomNavClick
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                MiraSnackbar(
                    snackbarData = data
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        NavigableCircuitContent(
            navigator = navigator,
            backStack = backStack,
            circuit = circuit,
            decoratorFactory = GestureNavigationDecorationFactory(
                onBackInvoked = { navigator.pop() }
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}
