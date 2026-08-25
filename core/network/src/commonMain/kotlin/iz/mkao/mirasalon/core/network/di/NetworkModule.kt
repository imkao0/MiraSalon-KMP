package iz.mkao.mirasalon.core.network.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import iz.mkao.mirasalon.core.network.client.KtorPromoApi
import iz.mkao.mirasalon.core.network.client.PromoApi
import iz.mkao.mirasalon.core.network.client.SalonApiSettings
import iz.mkao.mirasalon.core.network.client.SalonNetworkClientFactory
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.client.SettingsTokenProvider
import iz.mkao.mirasalon.core.network.client.provideBaseUrl
import iz.mkao.mirasalon.core.network.client.providePlatformHttpClientEngine
import iz.mkao.mirasalon.core.network.client.provideWebSocketUrl
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.SalonApiConfig
import iz.mkao.mirasalon.core.network.util.SalonNetworkLogger
import org.koin.dsl.module

val networkModule = module {
    single { SalonApiSettings(get()) }

    single<SalonApiConfig> {
        val apiSettings: SalonApiSettings = get()
        val baseUrl = apiSettings.getBaseUrl(provideBaseUrl())
        ApiEndpoints.setBaseUrl(baseUrl)
        SalonApiConfig(
            baseUrl = baseUrl,
            webSocketUrl = provideWebSocketUrl(),
            enableLogging = true
        )
    }

    single<SalonTokenProvider> {
        SettingsTokenProvider(get())
    }

    single<HttpClient> {
        SalonNetworkClientFactory.create(
            config = get(),
            tokenProvider = get(),
        )
    }

    single<HttpClient>(qualifier = org.koin.core.qualifier.named("imageLoader")) {
        HttpClient(providePlatformHttpClientEngine()) {
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
            }
            install(Logging) {
                logger = SalonNetworkLogger
                level = LogLevel.INFO
            }
            // No Auth plugin, no custom validator, to avoid issues with public images
        }
    }

    single<PromoApi> { KtorPromoApi(get()) }
}
