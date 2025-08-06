package iz.mkao.mirasalon.feature.profile.domain.model

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val apiBaseUrl: String? = null
)
