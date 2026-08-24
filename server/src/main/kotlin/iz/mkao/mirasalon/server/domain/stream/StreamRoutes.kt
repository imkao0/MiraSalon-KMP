package iz.mkao.mirasalon.server.domain.stream

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.StreamTokenRequest
import iz.mkao.mirasalon.core.network.model.dto.StreamTokenResponse
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.service.NotificationService
import iz.mkao.mirasalon.server.service.StreamSyncService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

private val log = LoggerFactory.getLogger("StreamRoutes")

fun Route.streamRoutes(
    streamApiKey: String,
    streamApiSecret: String,
    streamAppId: String,
    userRepository: UserRepository,
    specialistRepository: SpecialistRepository,
    streamSyncService: StreamSyncService? = null,
    notificationService: NotificationService? = null,
    jwtIssuer: String = "mirasalon-server",
    tokenValidityHours: Long = 24
) {

    authenticate("auth-jwt", optional = true) {
    post("/token") {
        // We still check for authentication if we want to issue a REAL token
        val authenticatedUserId = call.principal<JWTPrincipal>()
            ?.payload?.getClaim("userId")?.asString()

        // ── Parse and validate request ──
        val request = try {
            call.receive<StreamTokenRequest>()
        } catch (e: Exception) {
            log.warn("Malformed stream token request: {}", e.message)
            return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = "Invalid request format")
            )
        }

        var requestedUserId = request.userId.takeIf { it.isNotBlank() }
        if (requestedUserId == null) {
            return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = "userId is required")
            )
        }

        // ── Verify user exists and is active ──
        var user = userRepository.findById(requestedUserId)
        var specialistDto: iz.mkao.mirasalon.core.network.model.dto.SpecialistDto? = null

        // Fallback: If not found as a standard User ID, check if the ID refers to a Specialist.
        if (user == null) {
            val fetchResult = specialistRepository.findById(requestedUserId)
            if (fetchResult is iz.mkao.mirasalon.server.data.repository.SpecialistFetchResult.Success) {
                specialistDto = fetchResult.specialist
                val linkedUserId = specialistDto.userId
                if (linkedUserId != null) {
                    requestedUserId = linkedUserId
                    user = userRepository.findById(requestedUserId)
                }
            }
        }

        // ── Sync to Stream ──
        try {
            if (user != null) {
                streamSyncService?.syncUser(
                    userId = user.id,
                    name = user.name,
                    role = user.role,
                    avatarUrl = user.avatarUrl
                )
            } else if (specialistDto != null) {
                // Sync specialist directly if no user linked
                streamSyncService?.syncUser(
                    userId = specialistDto.id,
                    name = specialistDto.name,
                    role = UserRole.SPECIALIST,
                    avatarUrl = specialistDto.imageUrl
                )
            } else {
                log.warn("Stream token requested for non‑existent user/specialist: {}", requestedUserId)
                return@post call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(success = false, error = "User not found")
                )
            }
        } catch (e: Exception) {
            log.error("Failed to sync {} during token request: {}", requestedUserId, e.message, e)
        }

        if (user != null && !user.isActive) {
            log.warn("Stream token requested for inactive user: {}", requestedUserId)
            return@post call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<Unit>(success = false, error = "User account is inactive")
            )
        }

        // ── Security: Validate user can request token for this userId ──
        val roleClaim = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
        val isAdmin = roleClaim?.let { UserRole.fromString(it) } == UserRole.ADMIN

        if (authenticatedUserId == null ||
            (!isAdmin && requestedUserId != authenticatedUserId)
        ) {
            log.info(
                "Public or cross-user Stream sync triggered for {}. " +
                "Returning sync confirmation without token.",
                requestedUserId
            )

            // RETURN 200 OK with empty token for cross‑user sync pings or
            // unauthenticated pings
            return@post call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = StreamTokenResponse(
                        token = "",
                        userId = requestedUserId,
                        apiKey = streamApiKey,
                        appId = streamAppId,
                        expiresAt = 0
                    )
                )
            )
        }

        // ── Generate token ──
        try {
            val algorithm = Algorithm.HMAC256(streamApiSecret)
            val now = Instant.now()
            val expiresAt = now.plus(tokenValidityHours, ChronoUnit.HOURS)

            val tokenBuilder = JWT.create()
                .withIssuer(jwtIssuer)
                .withAudience(streamAppId)
                .withClaim("user_id", requestedUserId)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)

            user?.let {
                tokenBuilder.withClaim("user_email", it.email)
                tokenBuilder.withClaim("user_role", it.role.name)
            } ?: specialistDto?.let {
                tokenBuilder.withClaim("user_role", UserRole.SPECIALIST.name)
            }

            val token = tokenBuilder.sign(algorithm)

            log.info(
                "Stream token issued for user {} (requested by {})",
                requestedUserId,
                authenticatedUserId
            )

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = StreamTokenResponse(
                        token = token,
                        userId = requestedUserId,
                        apiKey = streamApiKey,
                        appId = streamAppId,
                        expiresAt = expiresAt.toEpochMilli()
                    )
                )
            )
        } catch (e: Exception) {
            log.error("Failed to generate stream token for user {}", requestedUserId, e)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(success = false, error = "Failed to generate token")
            )
        }
    }

    /**
     * Webhook endpoint for Stream Chat events.
     * Stream calls this whenever a message is sent, a channel is created, etc.
     */
    post("/webhook") {
        val payload = try {
            call.receive<Map<String, Any?>>()
        } catch (e: Exception) {
            return@post call.respond(HttpStatusCode.BadRequest)
        }

        val eventType = payload["type"] as? String
        log.info("Received Stream webhook event: {}", eventType)

        if (eventType == "message.new") {
            @Suppress("UNCHECKED_CAST")
            val message = payload["message"] as? Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val user = payload["user"] as? Map<String, Any?>
            val cid = payload["cid"] as? String
            
            val text = message?.get("text") as? String ?: ""
            val senderName = user?.get("name") as? String ?: "Client"
            val senderAvatar = user?.get("image") as? String
            
            // Extract deterministic ID components if possible
            val normalizedCid = cid?.split(":")?.last() ?: cid ?: ""
            
            log.info("New message from {} in {}: {}", senderName, normalizedCid, text)

            // 1. Send to real-time notification socket (for snackbars/badges)
            notificationService?.sendChatNotification(
                userId = "admin", // In a real app, resolve the correct recipient
                senderName = senderName,
                senderAvatarUrl = senderAvatar,
                messageText = text,
                conversationId = normalizedCid
            )
        }

        call.respond(HttpStatusCode.OK)
    }
    }

    authenticate("auth-jwt") {

        post("/notify-reply") {
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
                conversationId = conversationId
            )
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Notification sent"))
        }
    }
}
