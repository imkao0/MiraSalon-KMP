package iz.mkao.mirasalon.server.websocket

import io.github.aakira.napier.Napier
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.SpecialistDto
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import iz.mkao.mirasalon.server.data.repository.MessageRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistFetchResult
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.data.tables.OutboxAudience
import iz.mkao.mirasalon.server.realtime.RealtimeSessionRegistry
import iz.mkao.mirasalon.server.service.NotificationService
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.util.UUID

/** Partitioned chat connection for scalability. */
fun Route.chatWebSocket(
    registry: RealtimeSessionRegistry,
    userRepository: UserRepository? = null,
    specialistRepository: SpecialistRepository? = null,
    notificationService: NotificationService? = null,
    messageRepository: MessageRepository? = null
) {
    authenticate("auth-jwt") {
        webSocket(ApiEndpoints.WebSocket.CHAT_PARTITION) {
            val chatId = call.parameters["chatId"]
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()

            if (chatId == null || userId == null) {
                Napier.w("[ChatSocket] Connection rejected: chatId=$chatId, userId=$userId")
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing chatId or userId"))
                return@webSocket
            }

            if (!isSupportedChatId(chatId)) {
                Napier.w("[ChatSocket] Rejected connection with non-deterministic chatId: $chatId from user $userId")
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid chatId"))
                return@webSocket
            }

            Napier.d("[ChatSocket] User $userId joining chat partition: $chatId")
            registry.registerChat(chatId, this)

            try {
                // Connected event
                val connectedEvent = DomainEvent.Connected(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    message = "Joined chat partition: $chatId"
                )
                send(Frame.Text(DomainEventCodec.encode(connectedEvent)))

                // Fetch history from DB
                val history = messageRepository?.getHistory(chatId).orEmpty().map { msg ->
                    DomainEvent.ChatMessageReceived(
                        eventId = msg.id,
                        timestamp = msg.createdAt,
                        actorId = msg.senderId,
                        message = "History message",
                        conversationId = msg.chatId,
                        messageId = msg.id,
                        senderId = msg.senderId,
                        text = msg.content,
                        senderRole = msg.senderRole,
                        actingAsId = msg.actingAsId,
                        status = msg.status,
                        isInternal = msg.isInternal
                    )
                }

                if (history.isNotEmpty()) {
                    val historyEvent = DomainEvent.ChatHistory(
                        eventId = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        conversationId = chatId,
                        messages = history
                    )
                    send(Frame.Text(DomainEventCodec.encode(historyEvent)))
                }

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        Napier.v("[ChatSocket] Incoming frame from $userId in $chatId: $text")
                        if (text == "ping") {
                            send(Frame.Text("pong"))
                        } else {
                            val event = runCatching { DomainEventCodec.decode(text) }.getOrNull()
                            Napier.d("[ChatSocket] Decoded event from $userId: $event")
                            if (event is DomainEvent.ChatMessageReceived) {
                                // Session user ID as sender source of truth
                                val actualSenderId = userId

                                // Resolve participants
                                val participants = resolveParticipants(
                                    chatId = chatId,
                                    senderId = actualSenderId,
                                    userRepository = userRepository,
                                    specialistRepository = specialistRepository,
                                    messageRepository = messageRepository,
                                    specialistId = event.specialistId
                                )

                                val (specialist, customerId) = participants ?: (null to null)

                                // Recipient resolution
                                val recipientId = if (specialist != null && customerId != null) {
                                    if (actualSenderId == customerId || actualSenderId == specialist.userId) {
                                        // Swap recipient/sender roles
                                        if (actualSenderId == customerId) specialist.userId ?: specialist.id else customerId
                                    } else {
                                        // Fallback for ID overlaps
                                        ChatUtils.parseParticipantIds(chatId)?.firstOrNull { it != actualSenderId } ?: "admin"
                                    }
                                } else {
                                    ChatUtils.parseParticipantIds(chatId)?.firstOrNull { it != actualSenderId } ?: "admin"
                                }

                                // Persist to DB
                                runCatching {
                                    messageRepository?.save(
                                        id = event.messageId,
                                        chatId = event.conversationId,
                                        senderId = actualSenderId,
                                        recipientId = recipientId,
                                        senderRole = event.senderRole,
                                        actingAsId = event.actingAsId,
                                        content = event.text,
                                        status = event.status,
                                        isInternal = event.isInternal,
                                        createdAt = event.timestamp
                                    )
                                }.onFailure { Napier.w("[ChatSocket] Failed to persist message ${event.messageId}", it) }

                                Napier.d("[ChatSocket] Dispatching message ${event.messageId} to $chatId and admins")
                                if (!event.isInternal) {
                                    registry.dispatch(event, targetChatId = chatId)
                                }
                                registry.broadcastToAdmins(event)

                                // Trigger notification
                                if (recipientId != actualSenderId && !event.isInternal) {
                                    val senderUser = userRepository?.findById(actualSenderId)
                                    
                                    // Name resolution
                                    val actingAsId = event.actingAsId
                                    val notificationSenderName = if (event.senderRole == "ADMIN" && actingAsId != null) {
                                        // Admin acting as specialist
                                        (specialistRepository?.findById(actingAsId) as? SpecialistFetchResult.Success)?.specialist?.name
                                            ?: (specialistRepository?.findByUserId(actingAsId) as? SpecialistFetchResult.Success)?.specialist?.name
                                            ?: specialist?.name 
                                            ?: "Specialist"
                                    } else if (event.senderRole == "SPECIALIST" || event.senderRole == "ADMIN") {
                                        specialist?.name ?: senderUser?.name ?: event.senderName ?: "Specialist"
                                    } else {
                                        senderUser?.name ?: event.senderName ?: "User"
                                    }

                                    notificationService?.sendChatNotification(
                                        userId = recipientId,
                                        senderName = notificationSenderName,
                                        senderAvatarUrl = senderUser?.avatarUrl ?: event.senderAvatarUrl,
                                        messageText = event.text,
                                        conversationId = chatId,
                                        messageId = event.messageId,
                                        isInternal = event.isInternal,
                                        audience = OutboxAudience.CLIENT.name
                                    )
                                }

                                // Notify Admin if Client is texting a Specialist
                                if (event.senderRole == "CLIENT" && recipientId != "admin" && !event.isInternal) {
                                    val senderName = event.senderName ?: userRepository?.findById(event.senderId)?.name ?: "Client"
                                    val targetName = specialist?.name ?: "Specialist"
                                    notificationService?.sendChatNotification(
                                        userId = "admin",
                                        senderName = "Chat Monitor",
                                        messageText = "$senderName messaged $targetName: ${event.text}",
                                        conversationId = chatId,
                                        messageId = event.messageId,
                                        audience = OutboxAudience.ADMIN.name
                                    )
                                }
                            } else if (event is DomainEvent.ChatSeen) {
                                runCatching {
                                    messageRepository?.updateStatus(event.eventId, "READ")
                                }.onFailure { Napier.w("[ChatSocket] Failed to update status for message ${event.eventId}", it) }
                                
                                registry.dispatch(event, targetChatId = chatId)
                                registry.broadcastToAdmins(event)
                            } else if (event is DomainEvent.ChatTyping) {
                                registry.dispatch(event, targetChatId = chatId)
                                registry.broadcastToAdmins(event)
                            }
                        }
                    }
                }
            } catch (ignored: ClosedReceiveChannelException) {
            } catch (e: Exception) {
                Napier.w("Chat partition error for $chatId", e)
            } finally {
                registry.unregisterChat(chatId, this)
                Napier.d("User $userId left chat partition: $chatId")
            }
        }
    }
}

/** Validate deterministic or legacy chatId format. */
private fun isSupportedChatId(chatId: String): Boolean {
    if (!chatId.startsWith("chat_")) return false
    if (ChatUtils.isDeterministicChatId(chatId)) return true
    // Fallback format: allow legacy hash-based IDs or bare IDs that are long enough
    val body = chatId.removePrefix("chat_")
    return body.length >= 8
}

/** Resolve (specialist, customer) pair from chatId and history. */
private fun resolveParticipants(
    chatId: String,
    senderId: String,
    userRepository: UserRepository?,
    specialistRepository: SpecialistRepository?,
    messageRepository: MessageRepository? = null,
    specialistId: String? = null // Specialist ID from event for resolution
): Pair<SpecialistDto, String>? {
    val participantIds = ChatUtils.parseParticipantIds(chatId).orEmpty()
    
    // Infer from history for legacy IDs
    val historyIds = if (participantIds.isEmpty() && messageRepository != null) {
        messageRepository.getHistory(chatId, limit = 20).map { it.senderId }.distinct()
    } else emptyList()

    val ids = (participantIds + historyIds + senderId + (specialistId?.let { listOf(it) } ?: emptyList())).distinct()

    fun specialistFor(id: String): SpecialistDto? {
        // 1. Specialist ID
        (specialistRepository?.findById(id) as? SpecialistFetchResult.Success)?.let { return it.specialist }
        // 2. Linked User ID
        return (specialistRepository?.findByUserId(id) as? SpecialistFetchResult.Success)?.specialist
    }

    val specialist = ids.firstNotNullOfOrNull { specialistFor(it) }
        ?: ids.firstOrNull { it == "admin" || it == "me" }
            ?.let { SpecialistDto(id = it, name = "Admin", role = "Admin", salonId = "", userId = "admin") }

    if (specialist == null) {
        Napier.w("[ChatSocket] Failed to resolve specialist for candidates: $ids in chatId: $chatId")
        return null
    }

    val customerId = ids.firstOrNull { it != specialist.id && it != specialist.userId }
        ?: ids.firstOrNull { it != senderId } // Fallback for very sparse data

    if (customerId == null) {
        Napier.w("[ChatSocket] Failed to resolve customer for candidates: $ids in chatId: $chatId")
        return null
    }

    return specialist to customerId
}
