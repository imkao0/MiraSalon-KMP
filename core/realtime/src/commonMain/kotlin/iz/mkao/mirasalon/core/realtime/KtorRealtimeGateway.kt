package iz.mkao.mirasalon.core.realtime

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.model.SalonApiConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

open class KtorRealtimeGateway(
    protected val httpClient: HttpClient,
    protected val config: SalonApiConfig,
    protected val tokenProvider: SalonTokenProvider,
) : RealtimeGateway {

    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    protected val json = Json { ignoreUnknownKeys = true }

    protected val _events = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    protected val _connectionState = MutableStateFlow(RealtimeConnectionState.Idle)
    override val connectionState: StateFlow<RealtimeConnectionState> = _connectionState.asStateFlow()

    protected var reconnectAttempt = 0
    protected var connectionJob: Job? = null
    protected val chatScopes = mutableMapOf<String, CoroutineScope>()
    protected val chatEventFlows = mutableMapOf<String, MutableSharedFlow<DomainEvent>>()
    protected val chatSessions = mutableMapOf<String, DefaultClientWebSocketSession>()

    override suspend fun connect() {
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch { runConnectionLoop() }
    }

    override suspend fun disconnect() {
        _connectionState.value = RealtimeConnectionState.Disconnected
        connectionJob?.cancel()
        connectionJob = null
        chatScopes.values.forEach { it.cancel() }
        chatScopes.clear()
        chatSessions.values.forEach { 
            runCatching { it.close() }
        }
        chatSessions.clear()
        chatEventFlows.clear()
    }

    override suspend fun connectToChat(chatId: String) {
        if (chatScopes.containsKey(chatId)) return
        val chatScope = CoroutineScope(SupervisorJob())
        chatScopes[chatId] = chatScope
        chatEventFlows.getOrPut(chatId) { MutableSharedFlow(extraBufferCapacity = 32) }
        chatScope.launch { runChatConnectionLoop(chatId) }
    }

    override suspend fun disconnectFromChat(chatId: String) {
        chatScopes.remove(chatId)?.cancel()
    }

    override fun observeChatEvents(chatId: String): Flow<DomainEvent> {
        return chatEventFlows.getOrPut(chatId) { MutableSharedFlow(extraBufferCapacity = 32) }
    }

    override suspend fun sendChatEvent(chatId: String, event: DomainEvent) {
        val session = chatSessions[chatId]
        if (session != null && session.isActive) {
            val payload = json.encodeToString(DomainEvent.serializer(), event)
            session.send(Frame.Text(payload))
        }
    }

    protected suspend fun runConnectionLoop() {
        while (coroutineContext.isActive) {
            _connectionState.value = if (reconnectAttempt == 0) {
                RealtimeConnectionState.Connecting
            } else {
                RealtimeConnectionState.Reconnecting
            }

            val session = runCatching {
                // In a strict architecture, this is for metadata/notifications ONLY.
                httpClient.webSocketSession(urlString = config.webSocketUrl)
            }.onFailure { if (it is CancellationException) throw it }.getOrNull()

            if (session == null) {
                awaitBackoff()
                continue
            }

            _connectionState.value = RealtimeConnectionState.Connected
            reconnectAttempt = 0

            runCatching {
                for (frame in session.incoming) {
                    if (frame is Frame.Text) {
                        val event = runCatching { json.decodeFromString<DomainEvent>(frame.readText()) }
                            .onFailure { if (it is CancellationException) throw it }
                            .getOrNull()
                        // This pipe no longer receives chat messages in the "Strict" server implementation.
                        if (event != null) _events.emit(event)
                    }
                }
            }.onFailure { if (it is CancellationException) throw it }

            session.close()
            if (!coroutineContext.isActive) return
            awaitBackoff()
        }
    }

    protected open suspend fun runChatConnectionLoop(chatId: String) {
        val chatUrl = config.webSocketUrl.replace("/ws/notifications", "/ws/chat/$chatId")
        var chatReconnectAttempt = 0
        val eventFlow = chatEventFlows[chatId]!!

        while (chatScopes[chatId]?.isActive == true) {
            val session = runCatching {
                httpClient.webSocketSession(urlString = chatUrl)
            }.onFailure { if (it is CancellationException) throw it }.getOrNull()

            if (session == null) {
                chatReconnectAttempt++
                delay((min(30, 1 shl min(chatReconnectAttempt, 5)) * 1000L).milliseconds)
                continue
            }

            chatReconnectAttempt = 0
            chatSessions[chatId] = session
            runCatching {
                for (frame in session.incoming) {
                    if (frame is Frame.Text) {
                        val event = runCatching { json.decodeFromString<DomainEvent>(frame.readText()) }
                            .onFailure { if (it is CancellationException) throw it }
                            .getOrNull()
                        if (event != null) {
                            eventFlow.emit(event)
                        }
                    }
                }
            }.onFailure { if (it is CancellationException) throw it }

            session.close()
            if (chatScopes[chatId]?.isActive != true) return
            delay(1000L.milliseconds)
        }
    }

    protected suspend fun awaitBackoff() {
        reconnectAttempt += 1
        val backoffSeconds = min(30, 1 shl min(reconnectAttempt, 5))
        delay((backoffSeconds * 1000L).milliseconds)
    }
}
