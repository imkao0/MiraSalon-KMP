package iz.mkao.mirasalon.server.realtime

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
        chatSessions.getOrPut(chatId) { mutableSetOf() }.add(session)
    }

    fun unregisterChat(chatId: String, session: DefaultWebSocketServerSession) {
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
            chatSessions[targetChatId]?.forEach { session ->
                try {
                    session.send(payload)
                } catch (e: Exception) { }
            }
        }
    }

    suspend fun broadcastToAdmins(event: DomainEvent) {
        val payload = DomainEventCodec.encode(event)
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
