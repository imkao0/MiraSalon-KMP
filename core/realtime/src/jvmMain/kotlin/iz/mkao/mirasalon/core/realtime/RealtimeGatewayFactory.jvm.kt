package iz.mkao.mirasalon.core.realtime

import io.ktor.client.HttpClient
import iz.mkao.mirasalon.core.domain.repository.ChatManager
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

actual fun createChatManager(
    httpClient: HttpClient,
    tokenProvider: SalonTokenProvider
): ChatManager {
    return KtorChatManager(httpClient)
}
