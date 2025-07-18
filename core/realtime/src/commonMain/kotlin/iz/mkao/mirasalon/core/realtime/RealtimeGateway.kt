package iz.mkao.mirasalon.core.realtime

import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import kotlinx.coroutines.flow.Flow

interface RealtimeGateway {
    val events: Flow<DomainEvent>
    val connectionState: Flow<RealtimeConnectionState>
    suspend fun connect()
    suspend fun disconnect()

    /**
     * Joins a specific chat partition as per the "Chat-Partitioned Architecture".
     * This establishes a dedicated connection for the given chatId.
     */
    suspend fun connectToChat(chatId: String)
    suspend fun disconnectFromChat(chatId: String)

    /**
     * Observes events specifically for a chat partition.
     * In a strict partitioned architecture, message events only arrive here.
     */
    fun observeChatEvents(chatId: String): Flow<DomainEvent>

    /**
     * Sends a chat event to a specific partition.
     */
    suspend fun sendChatEvent(chatId: String, event: DomainEvent)
}

enum class RealtimeConnectionState {
    Idle,
    Connecting,
    Connected,
    Reconnecting,
    Disconnected,
}
