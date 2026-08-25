package iz.mkao.mirasalon.server.service

import iz.mkao.mirasalon.server.data.repository.OutboxRepository
import iz.mkao.mirasalon.server.data.tables.OutboxAudience
import iz.mkao.mirasalon.server.realtime.RealtimeSessionRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

class OutboxDispatcher(
    private val outboxRepository: OutboxRepository,
    private val realtimeRegistry: RealtimeSessionRegistry,
    private val json: Json
) {
    private val log = LoggerFactory.getLogger(OutboxDispatcher::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            while (isActive) {
                try {
                    val pending = outboxRepository.fetchUndispatched()
                    if (pending.isNotEmpty()) {
                        pending.forEach { record ->
                            when {
                                // Admin-only events (stock alerts, specialist availability,
                                // review submissions, etc.) go exclusively to admin sessions,
                                // i.e. the desktop dashboard - never to iOS/Android clients.
                                record.audience == OutboxAudience.ADMIN ->
                                    realtimeRegistry.broadcastToAdmins(record.event)
                                // Client events with a specific recipient.
                                record.targetUserId != null ->
                                    realtimeRegistry.dispatch(record.targetUserId, record.event)
                                // Client events without a recipient (e.g. new promotions)
                                // are broadcast to every connected client app.
                                else -> realtimeRegistry.broadcastToClients(record.event)
                            }
                        }
                        outboxRepository.markDispatched(pending.map { it.id })
                    }
                } catch (e: Exception) {
                    log.error("Error in OutboxDispatcher: ${e.message}", e)
                }
                delay(1.seconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
