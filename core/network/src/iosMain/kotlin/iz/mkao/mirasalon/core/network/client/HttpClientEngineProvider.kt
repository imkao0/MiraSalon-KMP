package iz.mkao.mirasalon.core.network.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun providePlatformHttpClientEngine(): HttpClientEngine = Darwin.create()

/**
 * Using localhost (127.0.0.1) for iOS Simulator connectivity.
 * For physical devices, use your machine's LAN IP (e.g., 192.168.1.113).
 */
private const val BASE_URL = "http://192.168.1.113:8080/"

actual fun provideBaseUrl(): String = BASE_URL

actual fun provideWebSocketUrl(): String {
    val httpBase = BASE_URL.removeSuffix("/")
    val wsBase = when {
        httpBase.startsWith("https://") -> "wss://" + httpBase.removePrefix("https://")
        httpBase.startsWith("http://") -> "ws://" + httpBase.removePrefix("http://")
        else -> httpBase
    }
    return "$wsBase/ws/notifications"
}
