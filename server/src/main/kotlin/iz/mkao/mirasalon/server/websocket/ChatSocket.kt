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
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import iz.mkao.mirasalon.server.data.repository.SpecialistFetchResult
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.realtime.RealtimeSessionRegistry
import iz.mkao.mirasalon.server.service.NotificationService
import iz.mkao.mirasalon.server.service.StreamSyncService
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Instead of one global notification bus for all messages, users connect to a specific chatId partition.
 * This breaks O(N) complexity at the gateway layer in a distributed system.
 */
fun Route.chatWebSocket(
    registry: RealtimeSessionRegistry,
    streamSyncService: StreamSyncService? = null,
    userRepository: UserRepository? = null,
    specialistRepository: SpecialistRepository? = null,
    notificationService: NotificationService? = null
) {
    authenticate("auth-jwt") {
        webSocket(ApiEndpoints.WebSocket.CHAT_PARTITION) {
            val chatId = call.parameters["chatId"]
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()

            if (chatId == null || userId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing chatId or userId"))
                return@webSocket
            }

            Napier.d("User $userId joining chat partition: $chatId")
            registry.registerChat(chatId, this)

            try {
                val connectedEvent = DomainEvent.Connected(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    message = "Joined chat partition: $chatId"
                )
                send(Frame.Text(DomainEventCodec.encode(connectedEvent)))

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text == "ping") {
                            send(Frame.Text("pong"))
                        } else {
                            val event = runCatching { DomainEventCodec.decode(text) }.getOrNull()
                            if (event is DomainEvent.ChatMessageReceived) {
                                registry.dispatch(event, targetChatId = chatId)
                                registry.broadcastToAdmins(event)

                                // Trigger notification for dashboard (bridge)
                                val senderUser = userRepository?.findById(event.senderId)
                                notificationService?.sendChatNotification(
                                    userId = "admin", 
                                    senderName = senderUser?.name ?: "Client",
                                    senderAvatarUrl = senderUser?.avatarUrl,
                                    messageText = event.text,
                                    conversationId = chatId
                                )

                                streamSyncService?.let {
                                    launch {
                                        runCatching {
                                            mirrorToStream(
                                                chatId = chatId,
                                                event = event,
                                                streamSyncService = streamSyncService,
                                                userRepository = userRepository,
                                                specialistRepository = specialistRepository
                                            )
                                        }.onFailure { Napier.w("Stream mirror failed for $chatId", it) }
                                    }
                                }
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

/**
 * Resolves the two participants of [chatId] (customer + specialist) from the deterministic
 * chat id and forwards the message to [StreamSyncService.mirrorChatMessage].
 */
private suspend fun mirrorToStream(
    chatId: String,
    event: DomainEvent.ChatMessageReceived,
    streamSyncService: StreamSyncService,
    userRepository: UserRepository?,
    specialistRepository: SpecialistRepository?
) {
    val participantIds = ChatUtils.parseParticipantIds(chatId) ?: emptyList()

    // 1. Resolve Specialist (or Admin)
    val specialistId = participantIds.firstOrNull { it.startsWith("spec-") }
        ?: participantIds.firstOrNull { id ->
            specialistRepository?.findById(id) is SpecialistFetchResult.Success
        }
        ?: participantIds.firstOrNull { it == "admin" || it == "me" }
        ?: event.senderId.takeIf { it.startsWith("spec-") || it == "admin" }
        ?: return

    // 2. Resolve Customer
    val customerId = participantIds.firstOrNull { it != specialistId }
        ?: event.senderId.takeIf { it != specialistId }
        ?: return

    val specialistName = when {
        specialistId == "admin" || specialistId == "me" -> "Admin"
        else -> (specialistRepository?.findById(specialistId) as? SpecialistFetchResult.Success)
            ?.specialist?.name ?: specialistId
    }

    val customerUser = userRepository?.findById(customerId)
    val customerName = customerUser?.name ?: customerId
    val customerAvatar = customerUser?.avatarUrl

    streamSyncService.mirrorChatMessage(
        channelId = chatId,
        customerId = customerId,
        customerName = customerName,
        customerAvatarUrl = customerAvatar,
        specialistId = specialistId,
        specialistName = specialistName,
        senderId = event.senderId,
        text = event.text,
        messageId = event.messageId
    )
}
