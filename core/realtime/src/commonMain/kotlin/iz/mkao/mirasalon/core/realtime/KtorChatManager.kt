package iz.mkao.mirasalon.core.realtime

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import iz.mkao.mirasalon.core.domain.model.chat.ChatMessage
import iz.mkao.mirasalon.core.domain.model.chat.ChatSession
import iz.mkao.mirasalon.core.domain.model.chat.MessageContent
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.ChatManager
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

class KtorChatManager(
    private val httpClient: HttpClient
) : ChatManager {

    private val _events = MutableSharedFlow<DomainEvent>()
    val events = _events.asSharedFlow()

    private val sendQueue = Channel<PendingMessage>(Channel.UNLIMITED)
    private var activeSession: DefaultClientWebSocketSession? = null

    private fun generateId(): String = Random.nextLong().toString(16)

    private data class PendingMessage(
        val event: DomainEvent,
        val result: CompletableDeferred<Outcome<Unit>>
    )

    override fun getChannels(): Flow<List<ChatSession>> = flow {
        try {
            val response = httpClient.get("v1/api/chat/conversations")
                .body<ApiResponse<List<ChatSession>>>()
            if (response.success) {
                emit(response.data ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Napier.e(tag = "KtorChatManager", throwable = e) { "Failed to fetch conversations" }
            emit(emptyList())
        }
    }

    /**
     * Watches a specific channel. this triggers the WebSocket
     * connection and keeps it active as long as the flow is collected.
     */
    override fun watchChannel(chatId: String): Flow<List<ChatMessage>> = channelFlow {
        // Start connection for this channel
        val connectionJob = launch {
            try {
                connect(chatId)
            } catch (e: Exception) {
                Napier.e(tag = "KtorChatManager", throwable = e) { "WebSocket connection failed for $chatId" }
            }
        }
        
        // Initial empty state. History is usually fetched via getHistory or backfilled by connect.
        send(emptyList())

        awaitClose {
            connectionJob.cancel()
        }
    }

    override fun observeEvents(chatId: String): Flow<ChatMessage> = events
        .filter { event ->
            event is DomainEvent.ChatMessageReceived && event.conversationId == chatId
        }
        .map { event ->
            val e = event as DomainEvent.ChatMessageReceived
            ChatMessage(
                id = e.messageId,
                sessionId = e.conversationId,
                senderId = e.senderId,
                senderRole = e.senderRole,
                actingAsId = e.actingAsId,
                text = e.text,
                timestamp = e.timestamp,
                isFromAdmin = e.senderRole == "ADMIN",
                status = e.status,
                isInternal = e.isInternal,
                content = MessageContent.Text(e.text)
            )
        }

    override fun sendMessage(
        chatId: String,
        text: String,
        senderRole: String,
        actingAsId: String?
    ): Flow<Outcome<Unit>> = flow {
        val deferred = CompletableDeferred<Outcome<Unit>>()
        val now = Clock.System.now().toEpochMilliseconds()
        val event = DomainEvent.ChatMessageReceived(
            eventId = generateId(),
            timestamp = now,
            actorId = actingAsId ?: "me",
            message = "New message",
            conversationId = chatId,
            messageId = generateId(),
            senderId = actingAsId ?: "me",
            text = text,
            senderRole = senderRole,
            actingAsId = actingAsId
        )
        
        sendQueue.send(PendingMessage(event, deferred))
        emit(deferred.await())
    }

    override fun sendImageMessage(chatId: String, imageUrl: String, caption: String?, actingAsId: String?): Flow<Outcome<Unit>> = flow {
        val deferred = CompletableDeferred<Outcome<Unit>>()
        val now = Clock.System.now().toEpochMilliseconds()
        val event = DomainEvent.ChatMessageReceived(
            eventId = generateId(),
            timestamp = now,
            actorId = actingAsId ?: "me",
            message = "Image message",
            conversationId = chatId,
            messageId = generateId(),
            senderId = actingAsId ?: "me",
            text = "[Image] $caption",
            senderRole = "CLIENT",
            actingAsId = actingAsId
        )
        sendQueue.send(PendingMessage(event, deferred))
        emit(deferred.await())
    }

    override fun markRead(chatId: String): Flow<Outcome<Unit>> = flow {
        emit(Outcome.Success(Unit))
    }

    suspend fun connect(chatId: String) {
        val wsUrl = ApiEndpoints.WebSocket.CHAT_PARTITION.replace("{chatId}", chatId)
        httpClient.webSocket(wsUrl) {
            activeSession = this
            
            // Background sender job
            val senderJob = launch {
                for (pending in sendQueue) {
                    try {
                        send(DomainEventCodec.encode(pending.event))
                        pending.result.complete(Outcome.Success(Unit))
                    } catch (e: Exception) {
                        pending.result.complete(Outcome.Error(Failure.ServerError(0, e.message ?: "Send failed")))
                    }
                }
            }

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val event = DomainEventCodec.decode(text)
                            _events.emit(event)
                        } catch (e: Exception) {
                            Napier.w(tag = "KtorChatManager", throwable = e) { "Failed to decode WS event: $text" }
                        }
                    }
                }
            } finally {
                senderJob.cancel()
                activeSession = null
            }
        }
    }
}
