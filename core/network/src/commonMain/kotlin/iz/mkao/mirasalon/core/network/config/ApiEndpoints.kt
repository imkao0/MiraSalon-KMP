package iz.mkao.mirasalon.core.network.config

import io.github.aakira.napier.Napier
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

    const val DEFAULT_SERVICE_IMAGE = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?q=80&w=1000&auto=format&fit=crop"

    private val SERVICE_IMAGE_MAP = mapOf(
        "balayage" to "https://images.unsplash.com/photo-1519735777090-ec97162ec268?q=80&w=1000&auto=format&fit=crop",
        "highlight" to "https://images.unsplash.com/photo-1519735777090-ec97162ec268?q=80&w=1000&auto=format&fit=crop",
        "coloring" to "https://images.unsplash.com/photo-1605497746444-144c15143a57?q=80&w=1000&auto=format&fit=crop",
        "dye" to "https://images.unsplash.com/photo-1605497746444-144c15143a57?q=80&w=1000&auto=format&fit=crop",
        
        "haircut" to "https://images.unsplash.com/photo-1560066984-138dadb4c035?q=80&w=1000&auto=format&fit=crop",
        "barber" to "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?q=80&w=1000&auto=format&fit=crop",
        "beard" to "https://images.unsplash.com/photo-1621605815971-fbc98d665033?q=80&w=1000&auto=format&fit=crop",
        "shave" to "https://images.unsplash.com/photo-1593702288056-7927b442d0fa?q=80&w=1000&auto=format&fit=crop",
        "braid" to "https://images.unsplash.com/photo-1647382218568-12501a4e511f?q=80&w=1000&auto=format&fit=crop",
        "hair" to "https://images.unsplash.com/photo-1560066984-138dadb4c035?q=80&w=1000&auto=format&fit=crop",
        
        "gel" to "https://images.unsplash.com/photo-1604654894610-df49ff550cca?q=80&w=1000&auto=format&fit=crop",
        "acrylic" to "https://images.unsplash.com/photo-1604654894610-df49ff550cca?q=80&w=1000&auto=format&fit=crop",
        "manicure" to "https://images.unsplash.com/photo-1604654894610-df49ff550cca?q=80&w=1000&auto=format&fit=crop",
        "pedicure" to "https://images.unsplash.com/photo-1519014816548-bf5fe059798b?q=80&w=1000&auto=format&fit=crop",
        "nail" to "https://images.unsplash.com/photo-1604654894610-df49ff550cca?q=80&w=1000&auto=format&fit=crop",
        
        "facial" to "https://images.unsplash.com/photo-1570172619666-1123475974e7?q=80&w=1000&auto=format&fit=crop",
        "peel" to "https://images.unsplash.com/photo-1570172619666-1123475974e7?q=80&w=1000&auto=format&fit=crop",
        "dermabrasion" to "https://images.unsplash.com/photo-1570172619666-1123475974e7?q=80&w=1000&auto=format&fit=crop",
        "massage" to "https://images.unsplash.com/photo-1544161515-4ad6ce6db874?q=80&w=1000&auto=format&fit=crop",
        
        "brazilian" to "https://images.unsplash.com/photo-1596178065887-1198b6148b2b?q=80&w=1000&auto=format&fit=crop",
        "bikini" to "https://images.unsplash.com/photo-1596178065887-1198b6148b2b?q=80&w=1000&auto=format&fit=crop",
        "wax" to "https://images.unsplash.com/photo-1596178065887-1198b6148b2b?q=80&w=1000&auto=format&fit=crop",
        "epilation" to "https://images.unsplash.com/photo-1596178065887-1198b6148b2b?q=80&w=1000&auto=format&fit=crop",
        "laser" to "https://images.unsplash.com/photo-1552693673-1bf958298935?q=80&w=1000&auto=format&fit=crop",
        "thread" to "https://images.unsplash.com/photo-1512496015851-a90fb38ba796?q=80&w=1000&auto=format&fit=crop",
        
        "lash" to "https://images.unsplash.com/photo-1583001931036-fe235b805c56?q=80&w=1000&auto=format&fit=crop",
        "eyelash" to "https://images.unsplash.com/photo-1583001931036-fe235b805c56?q=80&w=1000&auto=format&fit=crop",
        "extension" to "https://images.unsplash.com/photo-1583001931036-fe235b805c56?q=80&w=1000&auto=format&fit=crop",
        
        "makeup" to "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?q=80&w=1000&auto=format&fit=crop",
        "skincare" to "https://images.unsplash.com/photo-1556228578-0d85b1a4d571?q=80&w=1000&auto=format&fit=crop",
        "body" to "https://images.unsplash.com/photo-1515377905703-c4788e51af15?q=80&w=1000&auto=format&fit=crop",
        "scrub" to "https://images.unsplash.com/photo-1515377905703-c4788e51af15?q=80&w=1000&auto=format&fit=crop",
        
        "blowout" to "https://images.unsplash.com/photo-1522337360788-8b13dee7c37e?q=80&w=1000&auto=format&fit=crop",
        "keratin" to "https://images.unsplash.com/photo-1522337360788-8b13dee7c37e?q=80&w=1000&auto=format&fit=crop",
        "olaplex" to "https://images.unsplash.com/photo-1522337360788-8b13dee7c37e?q=80&w=1000&auto=format&fit=crop",
        "styl" to "https://images.unsplash.com/photo-1522337360788-8b13dee7c37e?q=80&w=1000&auto=format&fit=crop",
        
        "bridal" to "https://images.unsplash.com/photo-1595152244507-27060a35877c?q=80&w=1000&auto=format&fit=crop",
        "wedding" to "https://images.unsplash.com/photo-1595152244507-27060a35877c?q=80&w=1000&auto=format&fit=crop",
        
        "therapy" to "https://images.unsplash.com/photo-1519823551278-64ac92734fb1?q=80&w=1000&auto=format&fit=crop",
        "treatment" to "https://images.unsplash.com/photo-1512290923902-8a9f81dc236c?q=80&w=1000&auto=format&fit=crop",
        "spa" to "https://images.unsplash.com/photo-1544161515-4ad6ce6db874?q=80&w=1000&auto=format&fit=crop"
    )

    fun getServicePlaceholder(name: String?): String {
        if (name.isNullOrBlank()) return DEFAULT_SERVICE_IMAGE
        val lower = name.lowercase()
        return SERVICE_IMAGE_MAP.entries.find { lower.contains(it.key) }?.value ?: DEFAULT_SERVICE_IMAGE
    }

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
