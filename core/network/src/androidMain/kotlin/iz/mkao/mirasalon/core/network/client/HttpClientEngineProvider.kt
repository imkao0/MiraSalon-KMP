package iz.mkao.mirasalon.core.network.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import iz.mkao.mirasalon.core.network.NetworkConfig

actual fun providePlatformHttpClientEngine(): HttpClientEngine = OkHttp.create()

/**
 * Base URL is dynamically generated based on Gradle properties.
 */
actual fun provideBaseUrl(): String = NetworkConfig.API_BASE_URL

actual fun provideWebSocketUrl(): String {
    val httpBase = NetworkConfig.API_BASE_URL.removeSuffix("/")
    val wsBase = when {
        httpBase.startsWith("https://") -> "wss://" + httpBase.removePrefix("https://")
        httpBase.startsWith("http://") -> "ws://" + httpBase.removePrefix("http://")
        else -> httpBase
    }
    return "$wsBase/ws/notifications"
}
