package iz.mkao.mirasalon.server.realtime

import io.github.aakira.napier.Napier
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.send
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class RealtimeSessionRegistry {
    private val sessions = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()
    private val chatSessions = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()
    private val adminSessions = mutableSetOf<DefaultWebSocketServerSession>()
    private val mutex = Mutex()

    fun register(userId: String, role: UserRole, session: DefaultWebSocketServerSession) {
        sessions.getOrPut(userId) { mutableSetOf() }.add(session)
        if (role == UserRole.ADMIN) {
            adminSessions.add(session)
        }
    }

    fun unregister(userId: String, session: DefaultWebSocketServerSession) {
        sessions[userId]?.remove(session)
        adminSessions.remove(session)
    }

    fun registerChat(chatId: String, session: DefaultWebSocketServerSession) {
        Napier.d("[SessionRegistry] Registering chat session for partition: $chatId")
        chatSessions.getOrPut(chatId) { mutableSetOf() }.add(session)
    }

    fun unregisterChat(chatId: String, session: DefaultWebSocketServerSession) {
        Napier.d("[SessionRegistry] Unregistering chat session for partition: $chatId")
        chatSessions[chatId]?.remove(session)
    }

    suspend fun dispatch(userId: String, payload: String) {
        sessions[userId]?.forEach { session ->
            try {
                session.send(payload)
            } catch (e: Exception) {
                // Session closed
            }
        }
    }

    suspend fun dispatch(event: DomainEvent, targetChatId: String? = null) {
        val payload = DomainEventCodec.encode(event)
        if (targetChatId != null) {
            val sessions = chatSessions[targetChatId]
            Napier.d("[SessionRegistry] Dispatching event to $targetChatId. Active sessions: ${sessions?.size ?: 0}")
            sessions?.forEach { session ->
                try {
                    session.send(payload)
                } catch (e: Exception) {
                    Napier.w("[SessionRegistry] Failed to send to a session in $targetChatId", e)
                }
            }
        }
    }

    suspend fun broadcastToClients(payload: String) = mutex.withLock {
        sessions.forEach { (userId, userSessions) ->
            userSessions.forEach { session ->
                if (!adminSessions.contains(session)) {
                    try {
                        session.send(payload)
                    } catch (e: Exception) {
                        // Session closed
                    }
                }
            }
        }
    }

    suspend fun broadcastToAdmins(event: DomainEvent) {
        val payload = DomainEventCodec.encode(event)
        io.github.aakira.napier.Napier.d("[SessionRegistry] Broadcasting event to admins. Active admin sessions: ${adminSessions.size}")
        broadcastToAdmins(payload)
    }

    suspend fun broadcastToAdmins(payload: String) = mutex.withLock {
        adminSessions.forEach { session ->
            try {
                session.send(payload)
            } catch (e: Exception) {
                // Session closed
            }
        }
    }
}
