package iz.mkao.mirasalon.core.network.client

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class SalonApiSettings(private val settings: Settings) {
    companion object {
        private const val KEY_API_BASE_URL = "api_base_url"
    }

    fun getBaseUrl(fallback: String): String {
        return settings.getStringOrNull(KEY_API_BASE_URL) ?: fallback
    }

    fun setBaseUrl(url: String?) {
        if (url == null) {
            settings.remove(KEY_API_BASE_URL)
        } else {
            settings[KEY_API_BASE_URL] = url
        }
    }
}
