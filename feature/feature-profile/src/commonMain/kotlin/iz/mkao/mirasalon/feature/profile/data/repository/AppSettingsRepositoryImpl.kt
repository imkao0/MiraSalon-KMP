package iz.mkao.mirasalon.feature.profile.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.get
import iz.mkao.mirasalon.feature.profile.domain.model.AppSettings
import iz.mkao.mirasalon.feature.profile.domain.model.AppTheme
import iz.mkao.mirasalon.feature.profile.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppSettingsRepositoryImpl(
    private val settings: Settings,
) : AppSettingsRepository {

    private val _settings = MutableStateFlow(loadSettings())
    private val settingsFlow: StateFlow<AppSettings> = _settings.asStateFlow()

    override fun observeSettings(): StateFlow<AppSettings> = settingsFlow

    override suspend fun setTheme(theme: AppTheme) {
        settings[KEY_THEME] = theme.name
        _settings.update { it.copy(theme = theme) }
    }

    override suspend fun setApiBaseUrl(url: String?) {
        if (url == null) {
            settings.remove(KEY_API_BASE_URL)
        } else {
            settings[KEY_API_BASE_URL] = url
        }
        _settings.update { it.copy(apiBaseUrl = url) }
    }

    private fun loadSettings(): AppSettings {
        val theme = (settings.get<String>(KEY_THEME) as String?)
            ?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
            ?: AppTheme.SYSTEM
        val apiBaseUrl = settings.getStringOrNull(KEY_API_BASE_URL)
        return AppSettings(theme = theme, apiBaseUrl = apiBaseUrl)
    }

    private companion object {
        const val KEY_THEME = "app_settings_theme"
        const val KEY_API_BASE_URL = "api_base_url"
    }
}
