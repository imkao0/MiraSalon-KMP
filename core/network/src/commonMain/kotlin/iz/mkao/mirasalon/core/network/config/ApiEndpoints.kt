package iz.mkao.mirasalon.core.network.config

import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.network.config.ApiEndpoints.configure
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * Resolves backend-relative resource paths (e.g. uploaded image paths) into
 * absolute URLs against the configured API base URL.
 *
 * The base URL is supplied by [iz.mkao.mirasalon.core.network.model.SalonApiConfig]
 * (see NetworkModule) and pushed here once at startup via [configure]. It is no
 * longer a hard-coded LAN address. If [configure] was never called, a safe
 * localhost default is used instead of a machine-specific address.
 */
object ApiEndpoints {

    private const val DEFAULT_BASE_URL = "http://192.168.1.113:8080"

    @Volatile
    private var baseUrl: String = DEFAULT_BASE_URL
    private val mutex = Mutex()

    fun baseUrl(): String = baseUrl

    suspend fun configure(url: String) = mutex.withLock {
        baseUrl = url.trim().removeSuffix("/").ifBlank { DEFAULT_BASE_URL }
    }

    fun setBaseUrl(url: String) {
        baseUrl = url.trim().removeSuffix("/").ifBlank { DEFAULT_BASE_URL }
    }

    fun resolveImageUrl(imagePath: String?): String? {
        if (imagePath.isNullOrBlank()) return null
        
        // If it's already an absolute URL, replace local-only hosts (localhost, 127.0.0.1, 10.0.2.2) 
        // with the actual configured base URL to ensure iOS devices can connect.
        val resolved = if (imagePath.startsWith("http")) {
            val base = baseUrl.removeSuffix("/")
            
            // Extract the host:port part of the base URL to replace local hostnames properly
            val baseParts = base.split("://")
            if (baseParts.size == 2) {
                val protocol = baseParts[0]
                val hostPort = baseParts[1]
                
                imagePath
                    .replace(Regex("https?://localhost(:[0-9]+)?"), "$protocol://$hostPort")
                    .replace(Regex("https?://127\\.0\\.0\\.1(:[0-9]+)?"), "$protocol://$hostPort")
                    .replace(Regex("https?://10\\.0\\.2\\.2(:[0-9]+)?"), "$protocol://$hostPort")
            } else {
                imagePath
            }
        } else {
            val base = baseUrl.removeSuffix("/")
            val path = imagePath.removePrefix("/")
            "$base/$path"
        }

        // Log to console for debugging
        println("ApiEndpoints: resolveImageUrl: base=$baseUrl, path=$imagePath -> $resolved")
        Napier.d(tag = "ApiEndpoints") { "resolveImageUrl: base=$baseUrl, path=$imagePath -> $resolved" }
        return resolved
    }

    object WebSocket {
        const val NOTIFICATIONS = "/ws/notifications"
        const val CHAT_PARTITION = "/ws/chat/{chatId}"
    }
}
