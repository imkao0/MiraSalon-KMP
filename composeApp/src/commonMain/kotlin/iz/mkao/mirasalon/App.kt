package iz.mkao.mirasalon

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import iz.mkao.mirasalon.core.designsystem.theme.MiraSalonTheme
import iz.mkao.mirasalon.feature.profile.domain.model.AppSettings
import iz.mkao.mirasalon.feature.profile.domain.model.AppTheme
import iz.mkao.mirasalon.feature.profile.domain.model.AppSettingsRepository
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@OptIn(ExperimentalCoilApi::class)
@Composable
@Preview
fun App() {
    val imageHttpClient: HttpClient = koinInject(named("imageLoader"))
    
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(imageHttpClient))
            }
            .build()
    }

    val appSettingsRepository: AppSettingsRepository = koinInject()
    val settings by appSettingsRepository.observeSettings()
        .collectAsStateWithLifecycle(initialValue = AppSettings())

    val isDark = when (settings.theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    StatusBarEffect(isDarkTheme = isDark)
    MiraSalonTheme(darkTheme = isDark) {
        MainScreen()
    }
}
