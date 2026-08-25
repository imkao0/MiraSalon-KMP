package iz.mkao.mirasalon.feature.chat.data.repository

import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.ChatManager
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.feature.chat.domain.model.ChatItem
import iz.mkao.mirasalon.feature.chat.domain.model.ChatMessage
import iz.mkao.mirasalon.feature.chat.domain.model.Conversation
import iz.mkao.mirasalon.feature.chat.domain.model.MessageStatus
import iz.mkao.mirasalon.feature.chat.domain.model.QuickAccessContact
import iz.mkao.mirasalon.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

class ChatRepositoryImpl(
    private val realtimeGateway: RealtimeGateway,
    private val chatManager: ChatManager,
    private val tokenProvider: SalonTokenProvider,
    private val specialistRepository: SpecialistRepository,
    private val repositoryScope: CoroutineScope
) : ChatRepository {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    init {
        observeRealtimeEvents()
        observeAuthChanges()
    }

    private fun observeAuthChanges() {
        repositoryScope.launch {
            var lastUserId: String? = null
            tokenProvider.observeUserId().collect { currentUserId ->
                Napier.d { "[ChatRepository] observeAuthChanges: userId changed from $lastUserId to $currentUserId" }
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
            }
        }
    }

    private fun fetchRemoteHistory() {
        repositoryScope.launch {
            Napier.d { "[ChatRepository] fetchRemoteHistory: Starting channel fetch" }
            chatManager.getChannels().collect { sessions ->
                Napier.d { "[ChatRepository] fetchRemoteHistory: Received ${sessions.size} sessions" }
                val newConversations = sessions.map { session ->
                    val lastMsg = session.lastMessage?.let { 
                        ChatMessage(
                            id = it.id,
                            senderId = it.senderId,
                            text = it.text,
                            timestampEpochSeconds = it.timestamp / 1000,
                            status = it.status.toMessageStatus()
                        )
                    }

                    // Pre-fill messages map with last message to avoid flicker
                    if (lastMsg != null) {
                        _messages.update { current ->
                            val list = current[session.id] ?: emptyList()
                            if (list.any { it.id == lastMsg.id }) current
                            else current + (session.id to (list + lastMsg).sortedBy { it.timestampEpochSeconds })
                        }
                    }

                    Conversation(
                        id = session.id,
                        participantName = session.participantName,
                        participantRole = session.participantRole,
                        participantImageUrl = session.participantAvatarUrl,
                        lastMessage = lastMsg,
                        unreadCount = session.unreadCount,
                        participantIds = session.memberIds
                    )
                }
                Napier.d { "[ChatRepository] fetchRemoteHistory: Updated conversations with ${newConversations.size} items" }
                _conversations.value = newConversations
            }
        }
    }

    private fun observeRealtimeEvents() {
        repositoryScope.launch {
            realtimeGateway.events.collect { event ->
                Napier.d { "[ChatRepo] Global event received: $event" }
                when (event) {
                    is DomainEvent.ChatMessageReceived -> {
                        val newMessage = ChatMessage(
                            id = event.messageId,
                            senderId = event.senderId,
                            text = event.text,
                            timestampEpochSeconds = event.timestamp,
                            status = event.status.toMessageStatus()
                        )

                        _messages.update { current ->
                            val list = current[event.conversationId] ?: emptyList()
                            if (list.any { it.id == newMessage.id }) current
                            else current + (event.conversationId to (list + newMessage))
                        }

                        updateConversation(event.conversationId, newMessage)
                    }
                    is DomainEvent.ChatHistory -> {
                        // Backfill persisted history (survives server restarts, syncs devices).
                        handleHistory(event)
                    }
                    is DomainEvent.ChatSeen -> {
                        _messages.update { current ->
                            val list = current[event.conversationId] ?: emptyList()
                            current + (event.conversationId to list.map { msg ->
                                if (msg.senderId != event.userId && msg.status != MessageStatus.READ) {
                                    msg.copy(status = MessageStatus.READ)
                                } else msg
                            })
                        }
                    }
                    is DomainEvent.NotificationReceived -> {
                        if ((event.notificationType == "CHAT_MESSAGE" || event.notificationType == "MESSAGE") && event.referenceId != null) {
                            val currentUserId = tokenProvider.userId()
                            if (event.actorId != null && event.actorId == currentUserId) return@collect

                            // Increment unread count for this conversation
                            _conversations.update { current ->
                                current.map {
                                    if (it.id == event.referenceId) {
                                        // Deduplicate: if the last message already matches this notification, don't increment
                                        val isDuplicate = if (event.messageId != null) it.lastMessage?.id == event.messageId
                                                         else it.lastMessage?.text == event.message
                                        if (isDuplicate) it
                                        else it.copy(unreadCount = it.unreadCount + 1)
                                    } else it
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
            val currentUserId = tokenProvider.userId()
            val existing = _conversations.value.find { it.id == conversationId }
            if (existing != null) {
                _conversations.update { current ->
                    current.map {
                        if (it.id == conversationId) {
                            val isNewMessage = it.lastMessage?.id != lastMessage.id
                            val isFromOther = lastMessage.senderId != currentUserId
                            val shouldIncrement = isNewMessage && isFromOther
                            
                            it.copy(
                                lastMessage = lastMessage,
                                unreadCount = if (shouldIncrement) it.unreadCount + 1 else it.unreadCount
                            )
                        } else it
                    }
                }
            } else {
                val participantIds = ChatUtils.parseParticipantIds(conversationId)

                // If the chat ID is hashed (can't parse participant IDs), try to find specialist by matching
                // against the sender ID or by checking all specialists
                val specialist = if (participantIds == null) {
                    // Try to find specialist by matching the sender ID against specialist userId or id
                    val senderId = lastMessage.senderId
                    (specialistRepository.getSpecialists() as? Outcome.Success)?.data?.find { spec ->
                        spec.userId == senderId || spec.id == senderId
                    }
                } else {
                    val specialistId = participantIds.firstOrNull { it != currentUserId }
                    if (specialistId != null) {
                        (specialistRepository.getSpecialist(specialistId) as? Outcome.Success)?.data
                    } else null
                }

                val newConversation = Conversation(
                    id = conversationId,
                    participantName = specialist?.name ?: "Specialist",
                    participantRole = specialist?.role,
                    participantImageUrl = specialist?.imageUrl,
                    lastMessage = lastMessage,
                    unreadCount = 1,
                    participantIds = participantIds ?: emptyList()
                )
                _conversations.update { it + newConversation }
            }
        }
    }

    override fun observeConversations(): Flow<List<Conversation>> = _conversations.asStateFlow()
        .onStart {
            repositoryScope.launch { fetchRemoteHistory() }
        }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        _messages.map { it[conversationId] ?: emptyList() }
            .onStart {
                val deterministicId = getDeterministicId(conversationId)
                Napier.d { "[Android Chat] Connecting to partition: $deterministicId (requested: $conversationId)" }
                realtimeGateway.connectToChat(deterministicId)

                repositoryScope.launch {
                    realtimeGateway.observeChatEvents(deterministicId)
                        .collect { event ->
                            when (event) {
                                is DomainEvent.ChatMessageReceived -> {
                                    Napier.d { "[Chat] Received message: ${event.messageId} from ${event.senderId} in conversation ${event.conversationId}" }
                                    handleIncomingMessage(event)
                                }
                                is DomainEvent.ChatHistory -> {
                                    Napier.d { "[Chat] Received history for ${event.conversationId}: ${event.messages.size} messages" }
                                    handleHistory(event)
                                }
                                else -> Unit
                            }
                        }
                }
            }
            .onCompletion {
                val deterministicId = getDeterministicId(conversationId)
                realtimeGateway.disconnectFromChat(deterministicId)
            }

    /**
     * Merges the server-persisted history (sent once on WebSocket connect) into local state.
     * Existing messages (e.g. optimistic sends) win on id conflicts; the merged list is
     * re-sorted chronologically so the UI renders a consistent timeline on every device.
     */
    private fun handleHistory(event: DomainEvent.ChatHistory) {
        val historyMessages = event.messages.map { msg ->
            ChatMessage(
                id = msg.messageId,
                senderId = msg.senderId,
                text = msg.text,
                timestampEpochSeconds = msg.timestamp / 1000,
                status = msg.status.toMessageStatus()
            )
        }

        _messages.update { current ->
            val existing = current[event.conversationId] ?: emptyList()
            val existingIds = existing.mapTo(HashSet()) { it.id }
            val merged = (historyMessages.filterNot { it.id in existingIds } + existing)
                .sortedBy { it.timestampEpochSeconds }
            current + (event.conversationId to merged)
        }

        val latestMessage = historyMessages.lastOrNull()
        if (latestMessage != null) {
            updateConversation(event.conversationId, latestMessage)
        }
    }

    private fun handleIncomingMessage(event: DomainEvent.ChatMessageReceived) {
        Napier.d { "[ChatRepo] Handling incoming message: ${event.messageId} from ${event.senderId} in ${event.conversationId}" }
        val newMessage = ChatMessage(
            id = event.messageId,
            senderId = event.senderId,
            text = event.text,
            timestampEpochSeconds = event.timestamp / 1000,
            status = event.status.toMessageStatus()
        )
        
        _messages.update { current ->
            val list = current[event.conversationId] ?: emptyList()
            if (list.any { it.id == newMessage.id }) current
            else current + (event.conversationId to (list + newMessage))
        }

        updateConversation(event.conversationId, newMessage)
    }

    override suspend fun sendMessage(conversationId: String, text: String): Result<Unit> {
        val deterministicId = getDeterministicId(conversationId)
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val userId = tokenProvider.userId() ?: "me"
        val userName = tokenProvider.userName()
        val userAvatarUrl = tokenProvider.userAvatarUrl()
        val messageId = "msg-${Random.nextLong()}"

        Napier.d { "[ChatRepo] Sending message to partition: $deterministicId (requested: $conversationId, userId=$userId, messageId=$messageId). Payload: $text" }

        // Try to extract participant IDs from chat ID for server-side resolution
        val participantIds = ChatUtils.parseParticipantIds(deterministicId)
        val specialistId = participantIds?.firstOrNull { it != userId }

        // We use the deterministicId for the WebSocket partition.
        val event = DomainEvent.ChatMessageReceived(
            eventId = "evt-${Random.nextLong()}",
            timestamp = nowMillis,
            actorId = userId,
            message = "Message from ${userName ?: userId}",
            conversationId = deterministicId,
            messageId = messageId,
            senderId = userId,
            text = text,
            senderName = userName,
            senderAvatarUrl = userAvatarUrl,
            specialistId = specialistId // Include specialist ID for server-side resolution
        )

        realtimeGateway.sendChatEvent(deterministicId, event)

        val newMessage = ChatMessage(
            id = messageId,
            senderId = userId,
            text = text,
            timestampEpochSeconds = nowMillis / 1000,
            status = MessageStatus.SENT
        )

        _messages.update { current ->
            val list = current[deterministicId] ?: emptyList()
            if (list.any { it.id == messageId }) current
            else current + (deterministicId to (list + newMessage))
        }

        val existing = _conversations.value.find { it.id == deterministicId }
        if (existing != null) {
            _conversations.update { current ->
                current.map {
                    if (it.id == deterministicId) it.copy(lastMessage = newMessage)
                    else it
                }
            }
        } else {
            val currentUserId = tokenProvider.userId()
            val participantIds = ChatUtils.parseParticipantIds(deterministicId)

            // If the chat ID is hashed (can't parse participant IDs), we need to find the specialist
            // by looking at the conversation's participantId if it was passed from the route
            val specialist = if (participantIds == null) {
                // Try to find specialist by checking all specialists
                (specialistRepository.getSpecialists() as? Outcome.Success)?.data?.find { spec ->
                    spec.userId == currentUserId || spec.id == currentUserId
                }
            } else {
                val specialistId = participantIds.firstOrNull { it != currentUserId }
                if (specialistId != null) {
                    (specialistRepository.getSpecialist(specialistId) as? Outcome.Success)?.data
                } else null
            }

            val newConversation = Conversation(
                id = deterministicId,
                participantName = specialist?.name ?: "Specialist",
                participantRole = specialist?.role,
                participantImageUrl = specialist?.imageUrl,
                lastMessage = newMessage,
                unreadCount = 0,
                participantIds = participantIds ?: listOfNotNull(specialist?.id, currentUserId)
            )
            _conversations.update { it + newConversation }
        }
        
        return Result.success(Unit)
    }

    override suspend fun markAsRead(conversationId: String) {
        val deterministicId = getDeterministicId(conversationId)
        Napier.d { "[Android Chat] markAsRead called for $deterministicId (requested: $conversationId). Sending 'seen' event." }
        val userId = tokenProvider.userId() ?: "me"
        
        // Update local state
        _conversations.update { current ->
            current.map {
                if (it.id == deterministicId) it.copy(unreadCount = 0)
                else it
            }
        }

        // Send 'seen' event via WebSocket
        repositoryScope.launch {
            realtimeGateway.sendChatEvent(
                deterministicId,
                DomainEvent.ChatSeen(
                    eventId = "evt-seen-${Random.nextLong()}",
                    timestamp = Clock.System.now().epochSeconds,
                    actorId = userId,
                    conversationId = deterministicId,
                    userId = userId
                )
            )
        }
    }

    private suspend fun getDeterministicId(conversationId: String): String {
        if (ChatUtils.isDeterministicChatId(conversationId)) return conversationId
        val currentUserId = tokenProvider.userId() ?: return conversationId
        return ChatUtils.getDeterministicChatId(currentUserId, conversationId)
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

private fun String.toMessageStatus(): MessageStatus = runCatching {
    MessageStatus.valueOf(this)
}.getOrDefault(MessageStatus.SENT)
