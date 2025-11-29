package iz.mkao.mirasalon.core.network.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java

actual fun providePlatformHttpClientEngine(): HttpClientEngine = Java.create()

/**
 * Base URL is configuration-driven (system property `mirasalon.api.base-url`
 * or `MIRASALON_API_BASE_URL` env var) so no environment-specific address is
 * baked into the build. Localhost is only a development fallback.
 */
actual fun provideBaseUrl(): String = resolveBaseUrl()

actual fun provideWebSocketUrl(): String {
    System.getProperty("mirasalon.api.ws-url")
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }
    System.getenv("MIRASALON_API_WS_URL")
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }
    val httpBase = resolveBaseUrl().removeSuffix("/")
    val wsBase = when {
        httpBase.startsWith("https://") -> "wss://" + httpBase.removePrefix("https://")
        httpBase.startsWith("http://") -> "ws://" + httpBase.removePrefix("http://")
        else -> httpBase
    }
    return "$wsBase/ws/notifications"
}

private fun resolveBaseUrl(): String {
    val configured = System.getProperty("mirasalon.api.base-url")
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv("MIRASALON_API_BASE_URL")?.takeIf { it.isNotBlank() }
    val base = configured ?: "http://127.0.0.1:8080"
    return base.removeSuffix("/")
}
