package iz.mkao.mirasalon.feature.profile.domain.repository

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.feature.profile.domain.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow

interface NotificationPreferencesRepository {
    fun observePreferences(): Flow<NotificationPreferences>
    suspend fun updatePreferences(preferences: NotificationPreferences): Outcome<Unit>
}
