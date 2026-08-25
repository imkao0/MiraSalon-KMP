package iz.mkao.mirasalon.core.realtime

import io.ktor.client.HttpClient
import iz.mkao.mirasalon.core.domain.repository.ChatManager
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.model.SalonApiConfig

expect fun createRealtimeGateway(
    httpClient: HttpClient,
    config: SalonApiConfig,
    tokenProvider: SalonTokenProvider
): RealtimeGateway

expect fun createChatManager(
    httpClient: HttpClient,
    tokenProvider: SalonTokenProvider
): ChatManager
