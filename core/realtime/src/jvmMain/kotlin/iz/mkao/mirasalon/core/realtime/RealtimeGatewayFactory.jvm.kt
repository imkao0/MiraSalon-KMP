package iz.mkao.mirasalon.core.realtime

import io.ktor.client.HttpClient
import iz.mkao.mirasalon.core.domain.repository.StreamChatManager
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.model.SalonApiConfig
import org.koin.core.context.GlobalContext

actual fun createRealtimeGateway(
    httpClient: HttpClient,
    config: SalonApiConfig,
    tokenProvider: SalonTokenProvider
): RealtimeGateway {
    // JVM (Desktop/Server) can stick to Ktor gateway for now, 
    // or be extended to use the Java SDK if needed.
    return KtorRealtimeGateway(httpClient, config, tokenProvider)
}

actual fun createStreamChatManager(
    tokenProvider: SalonTokenProvider
): StreamChatManager {
    // For JVM/Desktop, we need the Stream credentials.
    // They are available in the Desktop's build config or passed via Koin.
    // Here we use Koin to get the config.
    val koin = GlobalContext.get()
    val config = koin.get<SalonApiConfig>()
    
    return DesktopStreamChatManager(
        apiKey = config.streamApiKey ?: "",
        apiSecret = config.streamApiSecret ?: ""
    )
}
