package iz.mkao.mirasalon.core.realtime

import io.ktor.client.HttpClient
import iz.mkao.mirasalon.core.domain.model.chat.ChatMessage
import iz.mkao.mirasalon.core.domain.model.chat.ChatSession
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.ChatManager
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.model.SalonApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual fun createRealtimeGateway(
    httpClient: HttpClient,
    config: SalonApiConfig,
    tokenProvider: SalonTokenProvider
): RealtimeGateway {
    // iOS will fallback to Ktor gateway for now as Stream KMP 
    // doesn't have a stable common/native client yet.
    return KtorRealtimeGateway(httpClient, config, tokenProvider)
}

actual fun createChatManager(
    httpClient: HttpClient,
    tokenProvider: SalonTokenProvider
): ChatManager {
    return KtorChatManager(httpClient)
}
