package iz.mkao.mirasalon.core.realtime

import android.content.Context
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.events.NewMessageEvent
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.models.Message
import io.getstream.chat.android.models.User
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.SalonApiConfig
import iz.mkao.mirasalon.core.network.model.dto.StreamTokenRequest
import iz.mkao.mirasalon.core.network.model.dto.StreamTokenResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class AndroidStreamRealtimeGateway(
    httpClient: HttpClient,
    config: SalonApiConfig,
    tokenProvider: SalonTokenProvider,
    private val context: Context
) : KtorRealtimeGateway(httpClient, config, tokenProvider) {

    private var streamClient: ChatClient? = null
    private val streamEvents = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 64)

    override suspend fun connect() {
        super.connect()
        initializeStream()
    }

    private suspend fun initializeStream() {
        val userId = tokenProvider.userId() ?: return
        val userName = tokenProvider.userName() ?: "User"
        val userAvatar = tokenProvider.userAvatarUrl() ?: ""

        val response = runCatching {
            httpClient.post("${config.baseUrl.removeSuffix("/")}/v1/api/stream/token") {
                contentType(ContentType.Application.Json)
                setBody(StreamTokenRequest(userId = userId))
            }.body<ApiResponse<StreamTokenResponse>>()
        }.getOrNull()

        val streamData = response?.takeIf { it.success }?.data ?: return

        if (!ChatClient.isInitialized) {
            val offlinePluginFactory = StreamOfflinePluginFactory(context)
            val statePluginFactory = StreamStatePluginFactory(config = StatePluginConfig(), appContext = context)

            ChatClient.Builder(streamData.apiKey, context)
                .withPlugins(offlinePluginFactory, statePluginFactory)
                .logLevel(ChatLogLevel.ALL)
                .build()
        }

        val client = ChatClient.instance()
        streamClient = client

        val user = User(
            id = userId,
            name = userName,
            image = userAvatar
        )

        client.connectUser(user, streamData.token).enqueue { result ->
            if (result.isSuccess) {
                observeStreamEvents()
            }
        }
    }

    private fun observeStreamEvents() {
        streamClient?.subscribe { event ->
            when (event) {
                is NewMessageEvent -> {
                    scope.launch {
                        // Normalize CID to exclude type prefix if present
                        val normalizedCid = event.cid.split(":").last()
                        val domainEvent = DomainEvent.ChatMessageReceived(
                            eventId = event.message.id,
                            timestamp = event.createdAt.time,
                            actorId = event.user.id,
                            message = "New message",
                            conversationId = normalizedCid,
                            messageId = event.message.id,
                            senderId = event.user.id,
                            text = event.message.text
                        )
                        streamEvents.emit(domainEvent)
                        _events.emit(domainEvent)
                    }
                }
                else -> {
                    // Handle other events if needed
                }
            }
        }
    }

    override fun observeChatEvents(chatId: String): Flow<DomainEvent> {
        val baseFlow = super.observeChatEvents(chatId)
        return merge(
            baseFlow,
            streamEvents.asSharedFlow().filter { 
                val eventCid = when (it) {
                    is DomainEvent.ChatMessageReceived -> it.conversationId
                    is DomainEvent.ChatSeen -> it.conversationId
                    else -> null
                }
                eventCid == chatId || eventCid?.endsWith(":$chatId") == true
            }
        )
    }

    override suspend fun sendChatEvent(chatId: String, event: DomainEvent) {
        // Send to custom WebSocket for real-time delivery to other clients (like Desktop)
        super.sendChatEvent(chatId, event)

        // Send to Stream for history persistence
        if (event is DomainEvent.ChatMessageReceived) {
            val parts = chatId.split(":")
            val channelType = if (parts.size > 1) parts[0] else "messaging"
            val channelId = if (parts.size > 1) parts[1] else parts[0]
            
            val userId = tokenProvider.userId() ?: ""
            val userName = tokenProvider.userName() ?: "User"
            val userAvatar = tokenProvider.userAvatarUrl() ?: ""
            
            // Resolve the specialist ID from the deterministic chatId if possible
            val participants = ChatUtils.parseParticipantIds(channelId)
            val specialistId = participants?.firstOrNull { it != userId }
            
            val channelClient = streamClient?.channel(channelType, channelId)
            
            // Ensure channel has metadata so Desktop Admin can filter/identify it
            if (channelClient != null && specialistId != null) {
                channelClient.create(
                    memberIds = listOf(userId, specialistId),
                    extraData = mapOf(
                        "customerId" to userId,
                        "customerName" to userName,
                        "customerAvatarUrl" to userAvatar,
                        "specialistId" to specialistId
                    )
                ).enqueue { result ->
                    if (result.isSuccess) {
                        channelClient.sendMessage(Message(text = event.text)).enqueue()
                    }
                }
            } else {
                channelClient?.sendMessage(Message(text = event.text))?.enqueue()
            }
        } else if (event is DomainEvent.ChatSeen) {
            val parts = chatId.split(":")
            val channelType = if (parts.size > 1) parts[0] else "messaging"
            val channelId = if (parts.size > 1) parts[1] else parts[0]
            
            streamClient?.channel(channelType, channelId)?.markRead()?.enqueue()
        }
    }
}
