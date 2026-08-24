package iz.mkao.mirasalon.server.domain.notification

import io.github.aakira.napier.Napier
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.NotificationDto
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import iz.mkao.mirasalon.server.data.tables.OutboxTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

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
                    OutboxTable.selectAll()
                        .where { OutboxTable.userId eq userId }
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
 * that cannot be decoded (e.g. older/legacy payloads) fall back to a generic
 * message instead of failing the whole request.
 */
private fun ResultRow.toNotificationDto(): NotificationDto {
    val eventId = this[OutboxTable.eventId]
    val createdAt = this[OutboxTable.createdAt]
    val dispatched = this[OutboxTable.dispatched]
    val payload = this[OutboxTable.payload]

    val event = runCatching { DomainEventCodec.decode(payload) }.getOrNull()

    val message = event?.message ?: "You have a new notification"
    val type = when (event) {
        is DomainEvent.NotificationReceived -> event.type
        is DomainEvent.ChatMessageReceived -> "MESSAGE"
        is DomainEvent.ChatSeen -> "MESSAGE"
        is DomainEvent.AppointmentReminder -> "REMINDER"
        is DomainEvent.BookingCreated,
        is DomainEvent.BookingUpdated -> "REMINDER"
        is DomainEvent.OrderCreated,
        is DomainEvent.OrderUpdated -> "PROMO"
        is DomainEvent.PromotionChanged -> "PROMO"
        is DomainEvent.ReviewSubmitted -> "COMMENT"
        else -> "MESSAGE"
    }

    val senderName = when (event) {
        is DomainEvent.ChatMessageReceived -> event.senderId
        is DomainEvent.NotificationReceived -> event.senderName ?: "Mira Salon"
        is DomainEvent.AppointmentReminder -> event.specialistName ?: "Mira Salon"
        else -> "Mira Salon"
    }

    val senderAvatarUrl = when (event) {
        is DomainEvent.NotificationReceived -> event.senderAvatarUrl
        is DomainEvent.AppointmentReminder -> event.specialistAvatarUrl
        else -> null
    }

    return NotificationDto(
        id = eventId,
        senderName = senderName,
        senderAvatarUrl = senderAvatarUrl,
        message = message,
        timestamp = createdAt,
        isUnread = !dispatched,
        type = type,
        thumbnail = senderAvatarUrl
    )
}
