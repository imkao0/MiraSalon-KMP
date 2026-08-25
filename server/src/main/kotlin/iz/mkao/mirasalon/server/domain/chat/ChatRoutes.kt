package iz.mkao.mirasalon.server.domain.chat

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.core.domain.model.chat.MessageContent
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.server.data.repository.MessageRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistFetchResult
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.data.tables.OutboxAudience
import iz.mkao.mirasalon.server.service.NotificationService
import iz.mkao.mirasalon.core.domain.model.chat.ChatMessage as DomainChatMessage
import iz.mkao.mirasalon.core.domain.model.chat.ChatSession as DomainChatSession

fun Route.chatRoutes(
    userRepository: UserRepository,
    specialistRepository: SpecialistRepository,
    messageRepository: MessageRepository,
    notificationService: NotificationService? = null
) {

    authenticate("auth-jwt") {

        /**
         * Returns all conversations for the authenticated user.
         * Admins receive ALL conversations in the system.
         */
        get("/chat/conversations") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            
            val roleClaim = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
            val role = roleClaim?.let { UserRole.fromString(it) }
            val isAdmin = role == UserRole.ADMIN

            val chatIds = if (isAdmin) {
                messageRepository.getAllConversations()
            } else {
                messageRepository.getConversations(userId)
            }

            val sessions = chatIds.mapNotNull { chatId ->
                val latestMsg = messageRepository.getLatestMessage(chatId) ?: return@mapNotNull null

                // Resolve participants robustly from deterministic ID, message metadata, and history
                val participantIds = (
                    (ChatUtils.parseParticipantIds(chatId) ?: emptyList()) + 
                    listOfNotNull(latestMsg.senderId, latestMsg.recipientId)
                ).distinct().toMutableList()

                if (participantIds.isEmpty()) {
                    // For hashed or legacy IDs, infer participants from message history
                    val history = messageRepository.getHistory(chatId, limit = 20)
                    participantIds.addAll(history.map { it.senderId })
                    participantIds.addAll(history.mapNotNull { it.recipientId })
                    participantIds.add(userId)
                }
                
                val distinctParticipants = participantIds.distinct()

                // Identify specialist and customer among participants
                var resolvedSpecialistId: String? = null
                var resolvedCustomerId: String? = null

                for (id in distinctParticipants) {
                    if (id.startsWith("spec-")) {
                        resolvedSpecialistId = id
                    } else {
                        // Check if this user ID is linked to a specialist
                        val isSpec = (specialistRepository.findByUserId(id) as? SpecialistFetchResult.Success) != null
                        if (isSpec) {
                            resolvedSpecialistId = id
                        } else if (id != "admin" && id != "me") {
                            resolvedCustomerId = id
                        }
                    }
                }

                // If we still can't resolve, use the other ID that isn't the current user
                val otherId = if (userId == resolvedCustomerId) {
                    resolvedSpecialistId ?: distinctParticipants.firstOrNull { it != userId } ?: "admin"
                } else if (userId == resolvedSpecialistId) {
                    resolvedCustomerId ?: distinctParticipants.firstOrNull { it != userId } ?: "admin"
                } else {
                    distinctParticipants.firstOrNull { it != userId } ?: "admin"
                }

                val otherUser = userRepository.findById(otherId)
                val otherSpecialist = (specialistRepository.findById(otherId) as? SpecialistFetchResult.Success)?.specialist
                        ?: (specialistRepository.findByUserId(otherId) as? SpecialistFetchResult.Success)?.specialist

                val name = otherSpecialist?.name ?: otherUser?.name ?: (if (otherId == "admin") "Mira Support" else otherId)
                val finalName = if (name.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))) {
                    "Specialist"
                } else name

                val avatar = otherSpecialist?.imageUrl ?: otherUser?.avatarUrl
                val participantRole = otherSpecialist?.role ?: (if (otherId == "admin") "Support" else "User")

                DomainChatSession(
                    id = chatId,
                    customerId = resolvedCustomerId ?: (if (resolvedSpecialistId != userId) userId else "unknown"),
                    customerName = if (userId == resolvedCustomerId) "Me" else (userRepository.findById(resolvedCustomerId ?: "")?.name ?: "Customer"),
                    specialistId = resolvedSpecialistId ?: "admin",
                    specialistName = otherSpecialist?.name ?: (if (userId == resolvedSpecialistId) "Me" else "Specialist"),
                    memberIds = distinctParticipants,
                    lastMessage = DomainChatMessage(
                        id = latestMsg.id,
                        sessionId = chatId,
                        senderId = latestMsg.senderId,
                        senderRole = latestMsg.senderRole,
                        actingAsId = latestMsg.actingAsId,
                        text = latestMsg.content,
                        timestamp = latestMsg.createdAt,
                        isFromAdmin = latestMsg.senderRole == "ADMIN" || latestMsg.senderId == "admin",
                        status = latestMsg.status,
                        isInternal = latestMsg.isInternal,
                        content = MessageContent.Text(latestMsg.content),
                        timeFormatted = ""
                    ),
                    lastMessageTime = latestMsg.createdAt,
                    unreadCount = messageRepository.getUnreadCount(chatId, userId),
                    participantName = finalName,
                    participantRole = participantRole,
                    participantAvatarUrl = avatar,
                    participantId = otherId
                )
            }.sortedByDescending { it.lastMessageTime }

            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = sessions))
        }

        post("/chat/notify-reply") {
            val authenticatedUserId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asString()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )

            val roleClaim = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
            val role = roleClaim?.let { UserRole.fromString(it) }

            if (role != UserRole.ADMIN && role != UserRole.SPECIALIST) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, error = "Access denied")
                )
            }

            val request = try {
                call.receive<Map<String, String>>()
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }

            val targetUserId = request["userId"]
            val senderName = request["senderName"] ?: "Staff"
            val senderAvatar = request["senderAvatar"]
            val messageText = request["message"]
            val conversationId = request["conversationId"]

            if (targetUserId == null) {
                return@post call.respond(HttpStatusCode.BadRequest, "userId required")
            }

            notificationService?.sendChatNotification(
                userId = targetUserId,
                senderName = senderName,
                senderAvatarUrl = senderAvatar,
                messageText = messageText,
                conversationId = conversationId,
                isInternal = request["isInternal"]?.toBoolean() ?: false,
                audience = OutboxAudience.CLIENT.name
            )
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Notification sent"))
        }
    }
}
