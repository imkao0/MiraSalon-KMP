package iz.mkao.mirasalon.server.data.repository

import iz.mkao.mirasalon.server.data.tables.MessagesTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * A single persisted chat message belonging to a deterministic chat partition.
 */
data class PersistedChatMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val recipientId: String?,
    val senderRole: String,
    val actingAsId: String?,
    val content: String,
    val status: String,
    val isInternal: Boolean,
    val createdAt: Long
)

/**
 * Durable store for chat messages.
 *
 * The WebSocket layer ([iz.mkao.mirasalon.server.websocket.chatWebSocket]) is a
 * stateless broadcast pipe: connected sessions live in memory and vanish on
 * restart. This repository is the source of truth that makes chat history
 * survive server restarts and lets newly connected clients (iOS, Android,
 * desktop) backfill the same conversation from the database instead of
 * starting from a blank slate.
 */
class MessageRepository {

    /**
     * Persists an incoming chat message. [createdAt] is epoch milliseconds.
     */
    fun save(
        id: String,
        chatId: String,
        senderId: String,
        recipientId: String? = null,
        senderRole: String = "CLIENT",
        actingAsId: String? = null,
        content: String,
        status: String = "SENT",
        isInternal: Boolean = false,
        createdAt: Long
    ) {
        transaction {
            MessagesTable.insert {
                it[this.id] = id
                it[this.chatId] = chatId
                it[this.senderId] = senderId
                it[this.recipientId] = recipientId
                it[this.senderRole] = senderRole
                it[this.actingAsId] = actingAsId
                it[this.content] = content
                it[this.status] = status
                it[this.isInternal] = isInternal
                it[this.createdAt] = createdAt
            }
        }
    }

    /**
     * Updates the status of a message (e.g., from SENT to DELIVERED or READ).
     */
    fun updateStatus(messageId: String, newStatus: String) {
        transaction {
            MessagesTable.update({ MessagesTable.id eq messageId }) {
                it[this.status] = newStatus
            }
        }
    }

    /**
     * Returns the most recent messages for [chatId] in ascending chronological
     * order, capped at [limit] (default [DEFAULT_HISTORY_LIMIT]).
     */
    fun getHistory(chatId: String, limit: Int = DEFAULT_HISTORY_LIMIT): List<PersistedChatMessage> =
        transaction {
            MessagesTable.selectAll()
                .where { MessagesTable.chatId eq chatId }
                .orderBy(MessagesTable.createdAt to SortOrder.DESC)
                .limit(limit)
                .map {
                    PersistedChatMessage(
                        id = it[MessagesTable.id],
                        chatId = it[MessagesTable.chatId],
                        senderId = it[MessagesTable.senderId],
                        recipientId = it[MessagesTable.recipientId],
                        senderRole = it[MessagesTable.senderRole],
                        actingAsId = it[MessagesTable.actingAsId],
                        content = it[MessagesTable.content],
                        status = it[MessagesTable.status],
                        isInternal = it[MessagesTable.isInternal],
                        createdAt = it[MessagesTable.createdAt]
                    )
                }
                .asReversed()
        }

    /**
     * Returns all unique chat IDs where [userId] is a participant.
     */
    fun getConversations(userId: String): List<String> =
        transaction {
            MessagesTable.selectAll()
                .where { (MessagesTable.senderId eq userId) or (MessagesTable.recipientId eq userId) or (MessagesTable.actingAsId eq userId) or (MessagesTable.chatId like "chat_%$userId%") }
                .map { it[MessagesTable.chatId] }
                .distinct()
        }

    /**
     * Returns all unique chat IDs across the system (for admins).
     */
    fun getAllConversations(): List<String> =
        transaction {
            MessagesTable.selectAll()
                .map { it[MessagesTable.chatId] }
                .distinct()
        }

    /**
     * Returns the latest message for a given [chatId].
     */
    fun getLatestMessage(chatId: String): PersistedChatMessage? =
        transaction {
            MessagesTable.selectAll()
                .where { MessagesTable.chatId eq chatId }
                .orderBy(MessagesTable.createdAt to SortOrder.DESC)
                .limit(1)
                .map {
                    PersistedChatMessage(
                        id = it[MessagesTable.id],
                        chatId = it[MessagesTable.chatId],
                        senderId = it[MessagesTable.senderId],
                        recipientId = it[MessagesTable.recipientId],
                        senderRole = it[MessagesTable.senderRole],
                        actingAsId = it[MessagesTable.actingAsId],
                        content = it[MessagesTable.content],
                        status = it[MessagesTable.status],
                        isInternal = it[MessagesTable.isInternal],
                        createdAt = it[MessagesTable.createdAt]
                    )
                }
                .singleOrNull()
        }

    /**
     * Returns the number of unread messages for [userId] in [chatId].
     */
    fun getUnreadCount(chatId: String, userId: String): Int = transaction {
        MessagesTable.selectAll().where {
            (MessagesTable.chatId eq chatId) and
            (MessagesTable.senderId neq userId) and
            (MessagesTable.status neq "READ")
        }.count().toInt()
    }

    companion object {
        const val DEFAULT_HISTORY_LIMIT = 200
    }
}
