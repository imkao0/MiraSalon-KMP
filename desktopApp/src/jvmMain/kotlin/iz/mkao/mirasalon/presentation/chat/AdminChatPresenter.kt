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
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.domain.repository.StreamChatManager
import iz.mkao.mirasalon.core.domain.repository.UploadRepository
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.data.local.TokenManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class AdminChatPresenter(
    private val chatManager: StreamChatManager,
    private val specialistRepository: SpecialistRepository,
    private val notificationRepository: NotificationRepository,
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
        val allChannels by remember(refreshChannelsTrigger) {
            chatManager.getChannels()
        }.collectAsState(initial = emptyList())
        
        // Watch for new messages globally via the server's notification WebSocket
        // (triggered by the Stream Webhook or Bridge)
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
            chatManager.watchChannel("messaging", selectedSessionId!!).collectAsState(emptyList())
        } else {
            remember { mutableStateOf(emptyList()) }
        }

        val websocketMessages = remember { SnapshotStateList<ChatMessage>() }

        // React to global events to update the UI and refresh channel list
        LaunchedEffect(globalEvents) {
            val event = globalEvents ?: return@LaunchedEffect
            
            // 1. If it's a message for the current chat, add it instantly
            if (event is DomainEvent.ChatMessageReceived && event.conversationId == selectedSessionId) {
                if (websocketMessages.none { it.id == event.messageId }) {
                    websocketMessages.add(event.toChatMessage())
                }
            }
            
            // 2. If it's any chat message, refresh the channel list to show the new/updated customer card
            if (event is DomainEvent.ChatMessageReceived || event is DomainEvent.NotificationReceived) {
                refreshChannelsTrigger++
            }
        }
        
        // Periodically refresh channel list as a fallback
        LaunchedEffect(Unit) {
            while (true) {
                delay(30.seconds)
                refreshChannelsTrigger++
            }
        }

        val messages = remember(streamMessages, websocketMessages.toList()) {
            (streamMessages + websocketMessages.toList()).distinctBy { it.id }.sortedBy { it.timestamp }
        }
        
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
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

        LaunchedEffect(selectedSessionId) {
            websocketMessages.clear()
            if (selectedSessionId != null) {



                val specialistId = resolveSessionSpecialistId(selectedSession, selectedSpecialistId) ?: "admin"

                val targetUserId = selectedSession?.customerId ?: selectedSession?.participantId ?: ""

                val deterministicId = ChatUtils.getDeterministicChatId(specialistId, targetUserId)

                Napier.d { "[Desktop Chat] Connecting to partition: $deterministicId (specialistId=$specialistId, customerId=$targetUserId, streamChannelId=$selectedSessionId)" }


                realtimeGateway.connectToChat(deterministicId)


                scope.launch {
                    realtimeGateway.observeChatEvents(deterministicId)
                        .collect { event ->
                            when (event) {
                                is DomainEvent.ChatMessageReceived -> {
                                    Napier.d { "[Desktop Chat] Received message: ${event.messageId} from ${event.senderId} in partition $deterministicId" }
                                    websocketMessages.add(event.toChatMessage())
                                }
                                is DomainEvent.ChatSeen -> {
                                    Napier.d { "[Desktop Chat] User ${event.userId} SEEN messages in partition $deterministicId" }

                                }
                                else -> {}
                            }
                        }
                }
            }
        }

        return AdminChatUiState(
            userName = userName,
            userAvatar = userAvatar,
            specialists = specialists,
            selectedSpecialistId = selectedSpecialistId,
            selectedSessionId = selectedSessionId,
            filteredChannels = filtered,
            selectedSession = selectedSession,
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
                        chatManager.markRead("messaging", event.id).collect { result ->
                            Napier.d { "[Desktop Chat] markRead result for ${event.id}: $result" }
                        }
                    }
                }
                is AdminChatEvent.NotifyChatReply -> scope.launch(ioDispatcher) {
                    notificationRepository.notifyChatReply(event.targetUserId, event.senderName, event.conversationId ?: selectedSessionId)
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
                    val specialist = specialists.find { it.id == selectedSpecialistId }
                    val asUserId = specialist?.id ?: selectedSpecialistId
                    val targetUserId = selectedSession?.participantId ?: ""
                    val senderName = specialist?.name ?: "Admin"

                    val imageBytes = pendingImageBytes
                    val imageName = pendingImageName
                    val messageText = inputText.trim()

                    when {

                        imageBytes != null && selectedSessionId != null -> {
                            val channelId = selectedSessionId!!
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
                                        val specialistId = resolveSessionSpecialistId(selectedSession, selectedSpecialistId) ?: "admin"
                                        val customerId = selectedSession?.customerId ?: selectedSession?.participantId ?: ""
                                        val deterministicId = ChatUtils.getDeterministicChatId(specialistId, customerId)

                                        Napier.d { "[Desktop Chat] Sending image message to Stream channel $channelId and WebSocket partition $deterministicId" }
                                        
                                        chatManager.sendImageMessage(
                                            type = "messaging",
                                            id = channelId,
                                            imageUrl = upload.data,
                                            caption = messageText.ifBlank { null },
                                            asUserId = specialistId
                                        ).collect { result ->
                                            Napier.d { "[Desktop Chat] sendImageMessage result: $result" }
                                            if (result is Outcome.Success) {
                                                if (selectedSessionId == channelId) {
                                                    inputText = ""
                                                    pendingImageBytes = null
                                                    pendingImageName = null
                                                    pendingImagePreview = null
                                                }
                                                notificationRepository.notifyChatReply(targetUserId, senderName, deterministicId)
                                            } else {
                                                error = "Failed to send image"
                                            }
                                        }
                                    }
                                    else -> error = "Failed to upload image"
                                }
                                isSendingImage = false
                            }
                        }
                        messageText.isNotBlank() && selectedSessionId != null -> {
                            val channelId = selectedSessionId!!
                            inputText = ""
                            scope.launch(ioDispatcher) {
                                val nowSeconds = java.time.Instant.now().epochSecond
                                val msgId = "msg-admin-${Random.nextLong()}"



                                val specialistId = resolveSessionSpecialistId(selectedSession, selectedSpecialistId) ?: "admin"
                                val customerId = selectedSession?.customerId ?: selectedSession?.participantId ?: ""
                                val deterministicId = ChatUtils.getDeterministicChatId(specialistId, customerId)

                                Napier.d { "[Desktop Chat] Sending text message to partition: $deterministicId. Specialist: $specialistId, Customer: $customerId. Monitoring for 'seen' status." }


                                realtimeGateway.sendChatEvent(
                                    deterministicId,
                                    DomainEvent.ChatMessageReceived(
                                        eventId = "evt-${Random.nextLong()}",
                                        timestamp = nowSeconds,
                                        actorId = specialistId,
                                        message = "Admin message",
                                        conversationId = deterministicId,
                                        messageId = msgId,
                                        senderId = specialistId,
                                        text = messageText
                                    )
                                )

                                chatManager.sendMessage(
                                    type = "messaging",
                                    id = channelId,
                                    text = messageText,
                                    asUserId = specialistId
                                ).collect { result ->
                                    Napier.d { "[Desktop Chat] sendMessage result: $result" }
                                    if (result is Outcome.Success) {
                                        notificationRepository.notifyChatReply(customerId, senderName, deterministicId)
                                    }
                                }
                            }
                        }
                        else -> Unit
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
    private fun resolveSessionSpecialistId(session: ChatSession?, fallbackSpecialistId: String?): String? {
        if (session == null) return fallbackSpecialistId
        return session.specialistId
            ?: ChatUtils.parseParticipantIds(session.id)?.firstOrNull { it.startsWith("spec-") }
            ?: session.memberIds.firstOrNull { it.startsWith("spec-") }
            ?: fallbackSpecialistId
    }

    private fun filterChannels(
        channels: List<ChatSession>,
        specialistId: String?,
        specialistList: List<Specialist>
    ): List<ChatSession> {
        if (specialistId == null) return emptyList()
        val specialist = specialistList.find { it.id == specialistId }
        val dbId = specialist?.id ?: specialistId
        
        return channels.filter { session ->
            val participants = ChatUtils.parseParticipantIds(session.id)
            if (participants != null) {
                if (dbId in participants || "spec-$dbId" in participants) return@filter true
            }

            val sessionSpecId = session.specialistId
            if (sessionSpecId != null) {
                if (sessionSpecId == dbId || sessionSpecId == "spec-$dbId") return@filter true
            }

            if (dbId in session.memberIds || "spec-$dbId" in session.memberIds) return@filter true

            val sessionSpecName = session.specialistName
            if (sessionSpecName != null && specialist?.name != null) {
                if (sessionSpecName.contains(specialist.name, ignoreCase = true)) return@filter true
            }
            
            false
        }
    }

    private fun DomainEvent.ChatMessageReceived.toChatMessage(): ChatMessage {
        val systemTZ = TimeZone.currentSystemDefault()
        val dateTime = Instant.fromEpochMilliseconds(timestamp * 1000).toLocalDateTime(systemTZ)
        val timeFormatted = "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
        
        return ChatMessage(
            id = messageId,
            sessionId = conversationId,
            senderId = senderId,
            text = text,
            timestamp = timestamp * 1000,
            isFromAdmin = false,
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
