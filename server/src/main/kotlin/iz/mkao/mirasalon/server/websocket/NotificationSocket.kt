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
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import iz.mkao.mirasalon.server.realtime.RealtimeSessionRegistry
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.util.UUID

fun Route.notificationWebSocket(registry: RealtimeSessionRegistry) {
    authenticate("auth-jwt") {
        webSocket(ApiEndpoints.WebSocket.NOTIFICATIONS) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()
            val roleStr = principal?.payload?.getClaim("role")?.asString()
            val role = UserRole.fromString(roleStr)

            if (userId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No user ID"))
                return@webSocket
            }

            registry.register(userId, role, this)

            try {
                val connectedEvent = DomainEvent.Connected(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    message = "Connected to MiraSalon Real-time Bus"
                )
                send(Frame.Text(DomainEventCodec.encode(connectedEvent)))

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text == "ping") {
                            send(Frame.Text("pong"))
                        }
                    }
                }
            } catch (ignored: ClosedReceiveChannelException) {
            } catch (e: Exception) {
                Napier.w("Notification socket error for user $userId", e)
            } finally {
                registry.unregister(userId, this)
            }
        }
    }
}
