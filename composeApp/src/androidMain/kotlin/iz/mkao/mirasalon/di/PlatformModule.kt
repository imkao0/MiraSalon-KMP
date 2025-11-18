package iz.mkao.mirasalon.di

import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<Settings> {
        val sharedPrefs = androidContext().getSharedPreferences("mira_salon_prefs", 0)
        SharedPreferencesSettings(sharedPrefs)
    }

    single<ImageLoader> {
        val imageHttpClient = get<HttpClient>(named("imageLoader"))
        ImageLoader.Builder(androidContext())
            .components {
                add(KtorNetworkFetcherFactory(imageHttpClient))
            }
            .build()
    }
}
