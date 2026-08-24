package iz.mkao.mirasalon.feature.chat.data.repository

import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.domain.repository.StreamChatManager
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.feature.chat.domain.model.ChatItem
import iz.mkao.mirasalon.feature.chat.domain.model.ChatMessage
import iz.mkao.mirasalon.feature.chat.domain.model.Conversation
import iz.mkao.mirasalon.feature.chat.domain.model.MessageStatus
import iz.mkao.mirasalon.feature.chat.domain.model.QuickAccessContact
import iz.mkao.mirasalon.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class ChatRepositoryImpl(
    private val realtimeGateway: RealtimeGateway,
    private val chatManager: StreamChatManager,
    private val tokenProvider: SalonTokenProvider,
    private val specialistRepository: SpecialistRepository,
    private val repositoryScope: CoroutineScope
) : ChatRepository {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    init {
        observeRealtimeEvents()
        repositoryScope.launch {
            var lastUserId: String? = null
            while (true) {
                val currentUserId = tokenProvider.userId()
                if (currentUserId != lastUserId) {
                    if (lastUserId != null) {
                        // User changed (logout/login), clear cache
                        _conversations.value = emptyList()
                        _messages.value = emptyMap()
                    }
                    if (currentUserId != null) {
                        fetchRemoteHistory()
                    }
                    lastUserId = currentUserId
                }
                delay(2000.milliseconds) // Poll for user changes every 2s
            }
        }
    }

    private fun fetchRemoteHistory() {
        repositoryScope.launch {
            chatManager.getChannels().collect { sessions ->
                val newConversations = sessions.map { session ->
                    val lastMsg = session.lastMessage?.let { 
                        ChatMessage(
                            id = it.id,
                            senderId = it.senderId,
                            text = it.text,
                            timestampEpochSeconds = it.timestamp / 1000,
                            status = MessageStatus.READ
                        )
                    }

                    Conversation(
                        id = session.id,
                        participantName = session.participantName,
                        participantRole = session.participantRole,
                        participantImageUrl = session.participantAvatarUrl,
                        lastMessage = lastMsg,
                        unreadCount = session.unreadCount
                    )
                }
                _conversations.value = newConversations
            }
        }
    }

    private fun observeRealtimeEvents() {
        repositoryScope.launch {
            realtimeGateway.events.collect { event ->
                when (event) {
                    is DomainEvent.ChatMessageReceived -> {
                        val newMessage = ChatMessage(
                            id = event.messageId,
                            senderId = event.senderId,
                            text = event.text,
                            timestampEpochSeconds = event.timestamp,
                            status = MessageStatus.READ
                        )

                        _messages.update { current ->
                            val list = current[event.conversationId] ?: emptyList()
                            if (list.any { it.id == newMessage.id }) current
                            else current + (event.conversationId to (list + newMessage))
                        }

                        updateConversation(event.conversationId, newMessage)
                    }
                    is DomainEvent.NotificationReceived -> {
                        if (event.type == "CHAT_MESSAGE" && event.referenceId != null) {
                            // Increment unread count for this conversation
                            _conversations.update { current ->
                                current.map {
                                    if (it.id == event.referenceId) it.copy(unreadCount = it.unreadCount + 1)
                                    else it
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateConversation(conversationId: String, lastMessage: ChatMessage) {
        repositoryScope.launch {
            val existing = _conversations.value.find { it.id == conversationId }
            if (existing != null) {
                _conversations.update { current ->
                    current.map {
                        if (it.id == conversationId) it.copy(lastMessage = lastMessage, unreadCount = it.unreadCount + 1)
                        else it
                    }
                }
            } else {
                val specialistId = ChatUtils.parseParticipantIds(conversationId)
                    ?.firstOrNull { it.startsWith("spec-") }
                    ?: conversationId
                val specialist = (specialistRepository.getSpecialist(specialistId) as? Outcome.Success)?.data
                val newConversation = Conversation(
                    id = conversationId,
                    participantName = specialist?.name ?: "Specialist",
                    participantRole = specialist?.role,
                    participantImageUrl = specialist?.imageUrl,
                    lastMessage = lastMessage,
                    unreadCount = 1
                )
                _conversations.update { it + newConversation }
            }
        }
    }

    override fun observeConversations(): Flow<List<Conversation>> = _conversations.asStateFlow()

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        _messages.map { it[conversationId] ?: emptyList() }
            .onStart {
                io.github.aakira.napier.Napier.d { "[Android Chat] Connecting to partition: $conversationId" }
                realtimeGateway.connectToChat(conversationId)

                repositoryScope.launch {
                    realtimeGateway.observeChatEvents(conversationId)
                        .filterIsInstance<DomainEvent.ChatMessageReceived>()
                        .collect { event ->
                            io.github.aakira.napier.Napier.d { "[Android Chat] Received message: ${event.messageId} from ${event.senderId} in conversation ${event.conversationId}" }
                            handleIncomingMessage(event)
                        }
                }
            }
            .onCompletion {
                realtimeGateway.disconnectFromChat(conversationId)
            }

    private fun handleIncomingMessage(event: DomainEvent.ChatMessageReceived) {
        io.github.aakira.napier.Napier.d { "[Android Chat] Handling incoming message: ${event.messageId} from ${event.senderId}" }
        val newMessage = ChatMessage(
            id = event.messageId,
            senderId = event.senderId,
            text = event.text,
            timestampEpochSeconds = event.timestamp,
            status = MessageStatus.READ
        )
        
        _messages.update { current ->
            val list = current[event.conversationId] ?: emptyList()
            // Ignore echoes of messages we already have (e.g. our own optimistic send).
            if (list.any { it.id == newMessage.id }) current
            else current + (event.conversationId to (list + newMessage))
        }

        updateConversation(event.conversationId, newMessage)
    }

    override suspend fun sendMessage(conversationId: String, text: String): Result<Unit> {
        val nowSeconds = Clock.System.now().epochSeconds
        val userId = tokenProvider.userId() ?: "me"
        val messageId = "msg-${Random.nextLong()}"

        io.github.aakira.napier.Napier.d { "[Android Chat] Sending message to partition: $conversationId (userId=$userId, messageId=$messageId)" }

        // We use the conversationId passed in, which should already be deterministic
        // if coming from SpecialistDetail or ChatList.
        val event = DomainEvent.ChatMessageReceived(
            eventId = "evt-${Random.nextLong()}",
            timestamp = nowSeconds,
            actorId = userId,
            message = "Message from $userId",
            conversationId = conversationId,
            messageId = messageId,
            senderId = userId,
            text = text
        )

        realtimeGateway.sendChatEvent(conversationId, event)

        val newMessage = ChatMessage(
            id = messageId,
            senderId = userId,
            text = text,
            timestampEpochSeconds = nowSeconds,
            status = MessageStatus.SENT
        )

        _messages.update { current ->
            val list = current[conversationId] ?: emptyList()
            if (list.any { it.id == messageId }) current
            else current + (conversationId to (list + newMessage))
        }

        val existing = _conversations.value.find { it.id == conversationId }
        if (existing != null) {
            _conversations.update { current ->
                current.map {
                    if (it.id == conversationId) it.copy(lastMessage = newMessage)
                    else it
                }
            }
        } else {
            val specialistId = ChatUtils.parseParticipantIds(conversationId)
                ?.firstOrNull { it.startsWith("spec-") }
                ?: conversationId
            val specialist = (specialistRepository.getSpecialist(specialistId) as? Outcome.Success)?.data
            val newConversation = Conversation(
                id = conversationId,
                participantName = specialist?.name ?: "Specialist",
                participantRole = specialist?.role,
                participantImageUrl = specialist?.imageUrl,
                lastMessage = newMessage,
                unreadCount = 0
            )
            _conversations.update { it + newConversation }
        }
        
        return Result.success(Unit)
    }

    override suspend fun markAsRead(conversationId: String) {
        io.github.aakira.napier.Napier.d { "[Android Chat] markAsRead called for $conversationId. Sending 'seen' event." }
        val userId = tokenProvider.userId() ?: "me"
        
        // Update local state
        _conversations.update { current ->
            current.map {
                if (it.id == conversationId) it.copy(unreadCount = 0)
                else it
            }
        }

        // Send 'seen' event via WebSocket
        repositoryScope.launch {
            realtimeGateway.sendChatEvent(
                conversationId,
                DomainEvent.ChatSeen(
                    eventId = "evt-seen-${Random.nextLong()}",
                    timestamp = Clock.System.now().epochSeconds,
                    actorId = userId,
                    conversationId = conversationId,
                    userId = userId
                )
            )
        }
    }

    override suspend fun deleteHistory() {
        _conversations.value = emptyList()
        _messages.value = emptyMap()
    }

    override suspend fun deleteChat(conversationId: String) {
        _conversations.update { current ->
            current.filterNot { it.id == conversationId }
        }
        _messages.update { current ->
            current.filterKeys { it != conversationId }
        }
    }

    override suspend fun getCurrentUserAvatarUrl(): String? = tokenProvider.userAvatarUrl()

    override suspend fun getCurrentUserName(): String? = tokenProvider.userName()

    override suspend fun getCurrentUserId(): String? = tokenProvider.userId()

    override suspend fun getChats(): List<ChatItem> = emptyList()

    override suspend fun getQuickAccessContacts(): List<QuickAccessContact> = emptyList()
}
