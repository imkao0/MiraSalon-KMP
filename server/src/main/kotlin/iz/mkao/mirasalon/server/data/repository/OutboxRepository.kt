package iz.mkao.mirasalon.server.data.repository

import iz.mkao.mirasalon.server.data.tables.OutboxTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class OutboxRecord(
    val id: String,
    val event: String,
    val targetUserId: String?
)

/**
 * Handles outbox events for reliable async dispatching (e.g., to WebSockets).
 */
class OutboxRepository {

    fun save(userId: String?, payload: String) = transaction {
        OutboxTable.insert {
            it[this.eventId] = UUID.randomUUID().toString()
            it[this.userId] = userId
            it[this.payload] = payload
            it[this.createdAt] = System.currentTimeMillis()
            it[this.dispatched] = false
        }
    }

    fun fetchUndispatched(): List<OutboxRecord> = transaction {
        OutboxTable.selectAll()
            .where { OutboxTable.dispatched eq false }
            .orderBy(OutboxTable.createdAt to SortOrder.ASC)
            .limit(50)
            .map {
                OutboxRecord(
                    id = it[OutboxTable.eventId],
                    event = it[OutboxTable.payload],
                    targetUserId = it[OutboxTable.userId]
                )
            }
    }

    fun markDispatched(ids: List<String>) = transaction {
        if (ids.isNotEmpty()) {
            OutboxTable.update({ OutboxTable.eventId inList ids }) {
                it[dispatched] = true
            }
        }
    }
}
