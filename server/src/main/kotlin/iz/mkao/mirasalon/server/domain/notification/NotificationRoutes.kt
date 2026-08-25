package iz.mkao.mirasalon.server.domain.notification

import io.github.aakira.napier.Napier
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.NotificationDto
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import iz.mkao.mirasalon.server.data.tables.OutboxAudience
import iz.mkao.mirasalon.server.data.tables.OutboxTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

fun Route.notificationRoutes() {
    authenticate("auth-jwt") {
        get {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()

            if (userId == null) {
                return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Unauthorized")
                )
            }

            try {
                val notifications = newSuspendedTransaction {
                    // Only CLIENT-audience rows are surfaced in the mobile
                    // notification screen. ADMIN-audience rows (stock alerts,
                    // specialist availability, review submissions, ...) are
                    // reserved for the admin desktop dashboard.
                    OutboxTable.selectAll()
                        .where {
                            ((OutboxTable.userId eq userId) or (OutboxTable.userId.isNull())) and
                            (OutboxTable.audience eq OutboxAudience.CLIENT.name)
                        }
                        .orderBy(OutboxTable.createdAt to SortOrder.DESC)
                        .limit(50)
                        .toList()
                        .mapNotNull { row ->
                            runCatching {
                                row.toNotificationDto()
                            }.onFailure { decodeError ->
                                Napier.e(
                                    "Skipping outbox event ${row[OutboxTable.eventId]}: failed to map notification",
                                    decodeError
                                )
                            }.getOrNull()
                        }
                }

                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = notifications))
            } catch (e: Exception) {
                Napier.e("Failed to fetch notifications for user $userId: ${e.message}", e)
                org.slf4j.LoggerFactory.getLogger("NotificationRoutes")
                    .error("Failed to fetch notifications for user $userId", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, error = "Failed to fetch notifications")
                )
            }
        }

        post("/{id}/read") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()
            val notificationId = call.parameters["id"]

            if (userId == null || notificationId == null) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Invalid request")
                )
            }

            try {
                newSuspendedTransaction {
                    OutboxTable.update({ (OutboxTable.eventId eq notificationId) and (OutboxTable.userId eq userId) }) {
                        it[isRead] = true
                    }
                }
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Notification marked as read"))
            } catch (e: Exception) {
                Napier.e("Failed to mark notification $notificationId as read", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, error = "Failed to mark notification as read")
                )
            }
        }

        delete {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()

            if (userId == null) {
                return@delete call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Unauthorized")
                )
            }

            try {
                newSuspendedTransaction {
                    OutboxTable.deleteWhere { OutboxTable.userId eq userId }
                }
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Notifications cleared"))
            } catch (e: Exception) {
                Napier.e("Failed to clear notifications for user $userId", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, error = "Failed to clear notifications")
                )
            }
        }
    }
}

/**
 * Maps a persisted outbox event to the client-facing [NotificationDto] contract.
 *
 * The payload stored in the outbox is a JSON-encoded [DomainEvent], so we decode
 * it and translate the event fields into what the mobile client expects. Events
 * that cannot be decoded fall back to a generic
 * message instead of failing the whole request.
 */
private fun ResultRow.toNotificationDto(): NotificationDto? {
    val eventId = this[OutboxTable.eventId]
    val createdAt = this[OutboxTable.createdAt]
    val isRead = this[OutboxTable.isRead]
    val payload = this[OutboxTable.payload]

    val event = runCatching { DomainEventCodec.decode(payload) }.getOrNull() ?: return null

    val type = when (event) {
        is DomainEvent.NotificationReceived -> event.notificationType
        is DomainEvent.ChatMessageReceived -> "MESSAGE"
        is DomainEvent.ChatSeen -> return null // Don't show "seen" in notification screen
        is DomainEvent.AppointmentReminder -> "REMINDER"
        is DomainEvent.BookingCreated,
        is DomainEvent.BookingUpdated -> "REMINDER"
        is DomainEvent.OrderCreated,
        is DomainEvent.OrderUpdated -> "PROMO"
        is DomainEvent.PromotionChanged -> "PROMO"
        is DomainEvent.ReviewSubmitted -> return null // Removed per principal engineer request
        else -> "MESSAGE"
    }

    val message = event.message

    val senderName = when (event) {
        is DomainEvent.ChatMessageReceived -> event.senderName ?: event.senderId
        is DomainEvent.NotificationReceived -> event.senderName ?: "Mira Salon"
        is DomainEvent.AppointmentReminder -> event.specialistName ?: "Mira Salon"
        is DomainEvent.BookingCreated -> event.specialistName ?: "Mira Salon"
        is DomainEvent.BookingUpdated -> event.specialistName ?: "Mira Salon"
        is DomainEvent.UserProfileUpdated -> event.userName ?: "Mira Salon"
        else -> "Mira Salon"
    }

    val senderAvatarUrl = when (event) {
        is DomainEvent.NotificationReceived -> event.senderAvatarUrl
        is DomainEvent.AppointmentReminder -> event.specialistAvatarUrl
        is DomainEvent.BookingCreated -> event.specialistAvatarUrl ?: event.customerAvatarUrl
        is DomainEvent.BookingUpdated -> event.specialistAvatarUrl ?: event.customerAvatarUrl
        is DomainEvent.ChatMessageReceived -> event.senderAvatarUrl
        is DomainEvent.UserProfileUpdated -> event.userAvatarUrl
        else -> null
    }

    return NotificationDto(
        id = eventId,
        senderName = senderName,
        senderAvatarUrl = senderAvatarUrl,
        message = message,
        timestamp = createdAt,
        isUnread = !isRead,
        type = type,
        thumbnail = senderAvatarUrl
    )
}
