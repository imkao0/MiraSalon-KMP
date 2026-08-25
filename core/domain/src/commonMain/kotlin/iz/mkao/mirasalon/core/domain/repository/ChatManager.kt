package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.chat.ChatMessage
import iz.mkao.mirasalon.core.domain.model.chat.ChatSession
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the chat system.
 * Abstracted to allow switching between different backends (WebSocket, Stream, etc.)
 */
interface ChatManager {
    /** Returns all available chat sessions for the current user. */
    fun getChannels(): Flow<List<ChatSession>>

    /** Observes a specific channel for real-time messages. */
    fun watchChannel(chatId: String): Flow<List<ChatMessage>>

    /** Sends a plain text message. */
    fun sendMessage(chatId: String, text: String, senderRole: String = "CLIENT", actingAsId: String? = null): Flow<Outcome<Unit>>

    /** Sends an image message. */
    fun sendImageMessage(chatId: String, imageUrl: String, caption: String? = null, actingAsId: String? = null): Flow<Outcome<Unit>>

    /** Marks all messages in a channel as read. */
    fun markRead(chatId: String): Flow<Outcome<Unit>>

    /** Subscribes to real-time events (messages, typing, etc.) */
    fun observeEvents(chatId: String): Flow<ChatMessage>
}
