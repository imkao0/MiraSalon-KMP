package iz.mkao.mirasalon.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.slack.circuit.runtime.presenter.Presenter
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.model.chat.ChatMessage
import iz.mkao.mirasalon.core.domain.model.chat.ChatSession
import iz.mkao.mirasalon.core.domain.model.chat.MessageContent
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.ChatManager
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.domain.repository.UploadRepository
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.data.local.TokenManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class AdminChatPresenter(
    private val chatManager: ChatManager,
    private val specialistRepository: SpecialistRepository,
    private val tokenManager: TokenManager,
    private val uploadRepository: UploadRepository,
    private val realtimeGateway: RealtimeGateway,
    private val ioDispatcher: CoroutineDispatcher,
    private val initialSessionId: String? = null
) : Presenter<AdminChatUiState> {

    @Composable
    override fun present(): AdminChatUiState {
        val session by tokenManager.session.collectAsState()
        val userName = session.name
        val userAvatar = session.avatarUrl

        var refreshChannelsTrigger by remember { mutableStateOf(0) }
        var refreshSpecialistsTrigger by remember { mutableStateOf(0) }
        
        val allChannels by remember(refreshChannelsTrigger) {
            chatManager.getChannels()
        }.collectAsState(initial = emptyList())
        
        // Watch for new messages globally via the server's notification WebSocket
        val globalEvents by realtimeGateway.events.collectAsState(null)
        
        var specialists by remember { mutableStateOf(emptyList<Specialist>()) }
        var selectedSpecialistId by remember { mutableStateOf<String?>(null) }
        var selectedSessionId by remember { mutableStateOf(initialSessionId) }
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        
        var inputText by remember { mutableStateOf("") }
        var pendingImageBytes by remember { mutableStateOf<ByteArray?>(null) }
        var pendingImageName by remember { mutableStateOf<String?>(null) }
        var pendingImagePreview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
        var isSendingImage by remember { mutableStateOf(false) }
        
        val filtered = filterChannels(allChannels, selectedSpecialistId, specialists)
        var selectedSession = filtered.find { it.id == selectedSessionId }

        if (selectedSession == null && selectedSessionId != null) {
            selectedSession = allChannels.find { it.id == selectedSessionId }
        }

        val streamMessages by if (selectedSessionId != null) {
            chatManager.watchChannel(selectedSessionId!!).collectAsState(emptyList())
        } else {
            remember { mutableStateOf(emptyList()) }
        }

        val websocketMessages = remember { SnapshotStateList<ChatMessage>() }

        // React to global events to update the UI and refresh channel list
        LaunchedEffect(globalEvents) {
            val event = globalEvents ?: return@LaunchedEffect
            Napier.d { "[Desktop Chat] Global event received: $event" }
            
            // 1. If it's a message for the current chat, add it instantly
            if (event is DomainEvent.ChatMessageReceived && event.conversationId == selectedSessionId) {
                if (websocketMessages.none { it.id == event.messageId }) {
                    websocketMessages.add(event.toChatMessage())
                }
            }
            
            // 2. If it's any chat message, refresh the channel list to show the new/updated customer card
            if (event is DomainEvent.ChatMessageReceived || event is DomainEvent.NotificationReceived) {
                refreshChannelsTrigger++
                
                // If it's a message for a specialist we don't have yet, refresh specialists too
                if (event is DomainEvent.ChatMessageReceived) {
                    val specId = event.specialistId ?: event.actingAsId
                    if (specId != null && specialists.none { it.id == specId || it.userId == specId || "spec-${it.id}" == specId }) {
                        refreshSpecialistsTrigger++
                    }
                }
            }
            
            // 3. If a specialist is created or changed, refresh the specialists list
            if (event is DomainEvent.SpecialistCreated || event is DomainEvent.SpecialistStatusChanged) {
                refreshSpecialistsTrigger++
            }
        }
        
        // Periodically refresh lists as a fallback
        LaunchedEffect(Unit) {
            while (true) {
                delay(30.seconds)
                refreshChannelsTrigger++
                refreshSpecialistsTrigger++
            }
        }

        val messages = remember(streamMessages, websocketMessages.toList()) {
            (streamMessages + websocketMessages.toList())
                .distinctBy { it.id }
                .sortedBy { it.timestamp }
        }
        
        val scope = rememberCoroutineScope()

        LaunchedEffect(refreshSpecialistsTrigger) {
            isLoading = true
            val result = specialistRepository.getSpecialists()
            if (result is Outcome.Success) {
                val list = result.data.filter { it.isActive }
                specialists = list
                if (selectedSpecialistId == null) selectedSpecialistId = list.firstOrNull()?.id
                isLoading = false
            } else {
                isLoading = false
                error = "Failed to load specialists"
            }
        }

        val specialistUnreadCounts = remember(allChannels, specialists) {
            specialists.associate { spec ->
                val count = allChannels.filter { session ->
                    // Reuse the same logic as filterChannels but for a specific specialist
                    isChannelForSpecialist(session, spec.id, specialists)
                }.sumOf { it.unreadCount }
                spec.id to count
            }
        }

        LaunchedEffect(selectedSessionId) {
            websocketMessages.clear()
            if (selectedSessionId != null) {
                // Determine the correct deterministic ID for this chat.
                // If the selected session already has a deterministic ID (e.g. from the sidebar), use it.
                val deterministicId = if (ChatUtils.isDeterministicChatId(selectedSessionId!!)) {
                    selectedSessionId!!
                } else {
                    val specialistId = resolveSessionSpecialistId(selectedSession, selectedSpecialistId, specialists) ?: "admin"
                    val targetUserId = selectedSession?.customerId ?: selectedSession?.participantId ?: ""
                    ChatUtils.getDeterministicChatId(specialistId, targetUserId)
                }

                Napier.d { "[Desktop Chat] Connecting to partition: $deterministicId (requested: $selectedSessionId)" }
                
                // IMPORTANT: Subscribe BEFORE connecting to avoid missing the History event
                val eventFlow = realtimeGateway.observeChatEvents(deterministicId)
                val collectJob = scope.launch {
                    eventFlow.collect { event ->
                        when (event) {
                            is DomainEvent.ChatMessageReceived -> {
                                Napier.d { "[Desktop Chat] Partition event received: ${event.messageId} from ${event.senderId} in partition $deterministicId" }
                                if (websocketMessages.none { it.id == event.messageId }) {
                                    websocketMessages.add(event.toChatMessage())
                                }
                            }
                            is DomainEvent.ChatHistory -> {
                                Napier.d { "[Desktop Chat] History received for $deterministicId: ${event.messages.size} messages" }
                                val mapped = event.messages.map { it.toChatMessage() }
                                val existingIds = websocketMessages.map { it.id }.toSet()
                                websocketMessages.addAll(mapped.filterNot { it.id in existingIds })
                            }
                            is DomainEvent.ChatSeen -> {
                                Napier.d { "[Desktop Chat] User ${event.userId} SEEN messages in partition $deterministicId" }
                            }
                            else -> {}
                        }
                    }
                }

                realtimeGateway.connectToChat(deterministicId)
                
                // When effect is cancelled (session changed), stop collecting
                try {
                    awaitCancellation()
                } finally {
                    collectJob.cancel()
                    realtimeGateway.disconnectFromChat(deterministicId)
                }
            }
        }

        return AdminChatUiState(
            currentUserId = session.userId,
            userName = userName,
            userAvatar = userAvatar,
            specialists = specialists,
            selectedSpecialistId = selectedSpecialistId,
            selectedSessionId = selectedSessionId,
            filteredChannels = filtered,
            selectedSession = selectedSession,
            specialistUnreadCounts = specialistUnreadCounts,
            messages = messages,
            inputText = inputText,
            pendingImageBytes = pendingImageBytes,
            pendingImageName = pendingImageName,
            pendingImagePreview = pendingImagePreview,
            isSendingImage = isSendingImage,
            isLoading = isLoading,
            error = error
        ) { event ->
            when (event) {
                is AdminChatEvent.SelectSpecialist -> {
                    selectedSpecialistId = event.id
                    // Auto-select the first customer for this specialist if available
                    val nextFiltered = filterChannels(allChannels, event.id, specialists)
                    selectedSessionId = nextFiltered.firstOrNull()?.id
                    inputText = ""
                    pendingImageBytes = null
                    pendingImageName = null
                    pendingImagePreview = null
                    error = null
                }
                is AdminChatEvent.SelectSession -> {
                    selectedSessionId = event.id
                    inputText = ""
                    pendingImageBytes = null
                    pendingImageName = null
                    pendingImagePreview = null
                    error = null
                    

                    Napier.d { "[Desktop Chat] Selecting session: ${event.id}, marking as read" }
                    scope.launch(ioDispatcher) {
                        chatManager.markRead(event.id).collect { result ->
                            Napier.d { "[Desktop Chat] markRead result for ${event.id}: $result" }
                        }
                    }
                }
                is AdminChatEvent.NotifyChatReply -> {
                    // Handled automatically by server upon message reception
                }
                is AdminChatEvent.InputTextChanged -> inputText = event.text
                is AdminChatEvent.ImageSelected -> {
                    pendingImageBytes = event.bytes
                    pendingImageName = event.fileName
                    pendingImagePreview = event.preview
                }
                AdminChatEvent.ClearPendingImage -> {
                    pendingImageBytes = null
                    pendingImageName = null
                    pendingImagePreview = null
                }
                AdminChatEvent.SendMessage -> {
                    val messageText = inputText.trim()
                    val selectedId = selectedSessionId
                    val session = selectedSession

                    if (selectedId != null) {
                        val specialist = specialists.find { it.id == selectedSpecialistId }
                        val senderName = specialist?.name ?: "Admin"
                        
                        // Use the same deterministic ID logic as in the history loading
                        val deterministicId = if (ChatUtils.isDeterministicChatId(selectedId)) {
                            selectedId
                        } else {
                            val specialistId = resolveSessionSpecialistId(session, selectedSpecialistId, specialists) ?: "admin"
                            val targetUserId = session?.customerId ?: session?.participantId ?: ""
                            ChatUtils.getDeterministicChatId(specialistId, targetUserId)
                        }

                        val imageBytes = pendingImageBytes
                        val imageName = pendingImageName

                        when {
                            imageBytes != null -> {
                                isSendingImage = true
                                error = null
                                scope.launch(ioDispatcher) {
                                    val upload = uploadRepository.uploadImage(
                                        bytes = imageBytes,
                                        fileName = imageName ?: "image.jpg",
                                        mimeType = imageName.mimeTypeFromName()
                                    )
                                    when (upload) {
                                        is Outcome.Success -> {
                                            val specialistId = resolveSessionSpecialistId(session, selectedSpecialistId, specialists) ?: "admin"
                                            val currentAdminId = tokenManager.userId() ?: "admin"

                                            Napier.d { "[Desktop Chat] Sending image message to WebSocket partition $deterministicId" }
                                            
                                            realtimeGateway.sendChatEvent(
                                                deterministicId,
                                                DomainEvent.ChatMessageReceived(
                                                    eventId = "evt-${Random.nextLong()}",
                                                    timestamp = System.currentTimeMillis(),
                                                    actorId = currentAdminId,
                                                    message = "Admin image message",
                                                    conversationId = deterministicId,
                                                    messageId = "msg-admin-${Random.nextLong()}",
                                                    senderId = currentAdminId,
                                                    text = "[Image] ${messageText.ifBlank { "" }}",
                                                    senderName = senderName,
                                                    senderAvatarUrl = specialist?.imageUrl ?: userAvatar,
                                                    senderRole = "ADMIN",
                                                    actingAsId = if (specialistId != "admin") specialistId else null
                                                )
                                            )

                                            if (selectedSessionId == selectedId) {
                                                inputText = ""
                                                pendingImageBytes = null
                                                pendingImageName = null
                                                pendingImagePreview = null
                                            }
                                        }
                                        else -> error = "Failed to upload image"
                                    }
                                    isSendingImage = false
                                }
                            }
                            messageText.isNotBlank() -> {
                                inputText = ""
                                scope.launch(ioDispatcher) {
                                    val nowMillis = System.currentTimeMillis()
                                    val msgId = "msg-admin-${Random.nextLong()}"
                                    val currentAdminId = tokenManager.userId() ?: "admin"
                                    val specialistId = resolveSessionSpecialistId(session, selectedSpecialistId, specialists) ?: "admin"
                                    val targetUserId = session?.customerId ?: session?.participantId ?: ""

                                    Napier.d { "[Desktop Chat] Sending text message to partition: $deterministicId (msgId=$msgId). Specialist: $specialistId, Customer: $targetUserId" }

                                    realtimeGateway.sendChatEvent(
                                        deterministicId,
                                        DomainEvent.ChatMessageReceived(
                                            eventId = "evt-${Random.nextLong()}",
                                            timestamp = nowMillis,
                                            actorId = currentAdminId,
                                            message = "Admin message",
                                            conversationId = deterministicId,
                                            messageId = msgId,
                                            senderId = currentAdminId,
                                            text = messageText,
                                            senderName = senderName,
                                            senderAvatarUrl = specialist?.imageUrl ?: userAvatar,
                                            senderRole = "ADMIN",
                                            actingAsId = if (specialistId != "admin") specialistId else null
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Resolves the specialist participant of [session] from the session itself
     * (deterministic chat id participants / members), falling back to the currently
     * selected specialist in the sidebar.
     */
    private fun resolveSessionSpecialistId(
        session: ChatSession?,
        fallbackSpecialistId: String?,
        specialistList: List<Specialist>
    ): String? {
        if (session == null) return fallbackSpecialistId
        
        // 1. Explicit specialistId from session
        session.specialistId?.let { return it }
        
        // 2. Look in participants / memberIds for something in specialistList
        val participants = ChatUtils.parseParticipantIds(session.id) ?: session.memberIds
        val found = participants.firstOrNull { id ->
            specialistList.any { it.id == id || it.userId == id || "spec-$id" == id }
        }
        if (found != null) return found
        
        // 3. Fallback to startsWith("spec-") for robustness
        return participants.firstOrNull { it.startsWith("spec-") }
            ?: fallbackSpecialistId
    }

    private fun filterChannels(
        channels: List<ChatSession>,
        specialistId: String?,
        specialistList: List<Specialist>
    ): List<ChatSession> {
        if (specialistId == null) return emptyList()
        return channels.filter { session ->
            isChannelForSpecialist(session, specialistId, specialistList)
        }
    }

    private fun isChannelForSpecialist(
        session: ChatSession,
        specialistId: String,
        specialistList: List<Specialist>
    ): Boolean {
        val specialist = specialistList.find { it.id == specialistId }
        val dbId = specialist?.id ?: specialistId
        val linkedUserId = specialist?.userId
        
        val participants = ChatUtils.parseParticipantIds(session.id)
        if (participants != null) {
            if (dbId in participants || "spec-$dbId" in participants) return true
            if (linkedUserId != null && linkedUserId in participants) return true
        }

        val sessionSpecId = session.specialistId
        if (sessionSpecId != null) {
            if (sessionSpecId == dbId || sessionSpecId == "spec-$dbId") return true
            if (linkedUserId != null && sessionSpecId == linkedUserId) return true
        }

        if (dbId in session.memberIds || "spec-$dbId" in session.memberIds) return true
        if (linkedUserId != null && linkedUserId in session.memberIds) return true

        val sessionSpecName = session.specialistName
        if (sessionSpecName != null && specialist?.name != null) {
            if (sessionSpecName.contains(specialist.name, ignoreCase = true)) return true
        }
        
        return false
    }

    private fun DomainEvent.ChatMessageReceived.toChatMessage(): ChatMessage {
        val systemTZ = TimeZone.currentSystemDefault()
        // Handle both seconds (legacy/some parts of system) and milliseconds (standard KMP)
        val timestampMs = if (timestamp > 1_000_000_000_000L) timestamp else timestamp * 1000
        val dateTime = Instant.fromEpochMilliseconds(timestampMs).toLocalDateTime(systemTZ)
        val timeFormatted = "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
        
        return ChatMessage(
            id = messageId,
            sessionId = conversationId,
            senderId = senderId,
            text = text,
            timestamp = timestampMs,
            isFromAdmin = senderRole == "ADMIN",
            status = status,
            isInternal = isInternal,
            content = MessageContent.Text(text),
            timeFormatted = timeFormatted
        )
    }
}

/** Maps a file name's extension to an image MIME type (chat supports jpg/png/webp). */
private fun String?.mimeTypeFromName(): String =
    when (this?.substringAfterLast('.', "")?.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }
