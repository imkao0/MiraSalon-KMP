package iz.mkao.mirasalon.core.realtime

import android.content.Context
import io.ktor.client.HttpClient
import iz.mkao.mirasalon.core.domain.repository.StreamChatManager
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.model.SalonApiConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun createRealtimeGateway(
    httpClient: HttpClient,
    config: SalonApiConfig,
    tokenProvider: SalonTokenProvider
): RealtimeGateway {
    // In Android, we need the context. We use Koin to get it since we are in a factory.
    val contextGetter = object : KoinComponent {
        val context: Context by inject()
    }
    return AndroidStreamRealtimeGateway(httpClient, config, tokenProvider, contextGetter.context)
}

actual fun createStreamChatManager(
    tokenProvider: SalonTokenProvider
): StreamChatManager {
    return AndroidStreamChatManager(tokenProvider)
}
