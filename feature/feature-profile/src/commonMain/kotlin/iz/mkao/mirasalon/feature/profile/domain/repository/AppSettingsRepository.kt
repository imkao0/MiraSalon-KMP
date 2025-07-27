package iz.mkao.mirasalon.feature.profile.domain.repository

import iz.mkao.mirasalon.feature.profile.domain.model.AppSettings
import iz.mkao.mirasalon.feature.profile.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setTheme(theme: AppTheme)
    suspend fun setApiBaseUrl(url: String?)
}
