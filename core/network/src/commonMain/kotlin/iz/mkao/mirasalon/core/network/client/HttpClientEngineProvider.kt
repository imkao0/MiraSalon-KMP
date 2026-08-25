package iz.mkao.mirasalon.core.network.client

import io.ktor.client.engine.HttpClientEngine

expect fun providePlatformHttpClientEngine(): HttpClientEngine

expect fun provideBaseUrl(): String
expect fun provideWebSocketUrl(): String
