package iz.mkao.mirasalon.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.util.FlowWrapper
import iz.mkao.mirasalon.core.common.util.wrap
import iz.mkao.mirasalon.core.domain.repository.CartRepository
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.client.provideBaseUrl
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatform
import kotlin.experimental.ExperimentalNativeApi

/**
 * Typed, Swift-friendly accessors over the shared Koin container.
 *
 * Swift should call [doInitKoinForIos] once from `iOSApp.init()` and then
 * obtain the shared [CircuitBridge] via [IosBridge.circuitBridge].
 * This removes the need for any Koin imports or `as!` force-casts on the
 * Swift side: every lookup is resolved and null-checked here in Kotlin.
 */

/** Starts Koin for the iOS app. Safe to call exactly once from `iOSApp.init()`. */
@OptIn(ExperimentalNativeApi::class)
fun doInitKoinForIos() {
    if (KoinPlatform.getKoinOrNull() == null) {
        initKoin()
        // Ensure ApiEndpoints is initialized with the correct base URL for iOS
        val baseUrl = provideBaseUrl()
        println("IosBridge: Initializing ApiEndpoints with base URL: $baseUrl")
        ApiEndpoints.setBaseUrl(baseUrl)
        if (Platform.isDebugBinary) {
            Napier.base(DebugAntilog())
        }
    }
}

object IosBridge {
    private val koin get() = KoinPlatform.getKoin()

    /** Shared Circuit instance, resolved (and null-checked) in Kotlin. */
    fun circuit(): Circuit = koin.get()

    /** Bridge used by SwiftUI to create presenter wrappers for screens. */
    fun circuitBridge(): CircuitBridge = CircuitBridge(circuit())

    /**
     * Create a presenter bridge for [screen].
     * Returns null (instead of crashing) when no presenter is registered,
     * so Swift can render a loud error state.
     */
    fun presenter(
        screen: Screen,
        navigator: Navigator
    ): CircuitPresenterKotlinBridge<CircuitUiState>? =
        circuitBridge().createPresenter(screen, navigator)

    /** Check if a session token exists (blocking call for Swift startup). */
    fun isLoggedIn(): Boolean {
        val tokenProvider: SalonTokenProvider = koin.get()
        return runBlocking { tokenProvider.accessToken() != null }
    }

    /** Trigger a global logout (blocking call for Swift). */
    fun logout() {
        val tokenProvider: SalonTokenProvider = koin.get()
        runBlocking { tokenProvider.onAuthenticationExpired() }
    }

    /** Observe the number of items in the cart. */
    fun observeCartCount(): FlowWrapper<Int> = koin.get<CartRepository>().observeCart().map { it.itemCount }.wrap()

    /** Observe the total number of unread chat messages. */
    fun observeUnreadChatCount(): FlowWrapper<Int> = koin.get<ChatRepository>().observeConversations().map { list ->
        list.sumOf { it.unreadCount }
    }.wrap()

    /** Observe the number of unread system notifications. */
    fun observeUnreadNotificationCount(): FlowWrapper<Int> = koin.get<NotificationRepository>().unreadCount.wrap()
}
