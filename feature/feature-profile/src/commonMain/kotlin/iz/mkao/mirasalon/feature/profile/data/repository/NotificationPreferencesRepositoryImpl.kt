package iz.mkao.mirasalon.feature.profile.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.get
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.feature.profile.domain.model.NotificationPreferences
import iz.mkao.mirasalon.feature.profile.domain.repository.NotificationPreferencesRepository
import iz.mkao.mirasalon.feature.profile.data.dto.NotificationPreferencesDto
import iz.mkao.mirasalon.feature.profile.data.mapper.toDomain
import iz.mkao.mirasalon.feature.profile.data.mapper.toDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NotificationPreferencesRepositoryImpl(
    private val settings: Settings,
) : NotificationPreferencesRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val _preferences = MutableStateFlow(loadPreferences())
    private val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    override fun observePreferences(): StateFlow<NotificationPreferences> = preferences

    override suspend fun updatePreferences(preferences: NotificationPreferences): Outcome<Unit> {
        settings[STORAGE_KEY] = json.encodeToString(preferences.toDto())
        _preferences.value = preferences
        return Outcome.Success(Unit)
    }

    private fun loadPreferences(): NotificationPreferences {
        val raw = (settings.get<String>(STORAGE_KEY) as String?) ?: return NotificationPreferences()
        return runCatching {
            json.decodeFromString<NotificationPreferencesDto>(raw).toDomain()
        }.getOrDefault(NotificationPreferences())
    }

    private companion object {
        const val STORAGE_KEY = "profile_notification_preferences"
    }
}
