package iz.mkao.mirasalon.core.realtime

import android.content.Context
import io.ktor.client.HttpClient
import iz.mkao.mirasalon.core.domain.repository.ChatManager
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.model.SalonApiConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun createRealtimeGateway(
    httpClient: HttpClient,
    config: SalonApiConfig,
    tokenProvider: SalonTokenProvider
): RealtimeGateway {
    val contextGetter = object : KoinComponent {
        val context: Context by inject()
    }
    return AndroidStreamRealtimeGateway(httpClient, config, tokenProvider, contextGetter.context)
}

actual fun createChatManager(
    httpClient: HttpClient,
    tokenProvider: SalonTokenProvider
): ChatManager {
    return KtorChatManager(httpClient)
}
