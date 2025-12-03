package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.chat.ChatMessage
import iz.mkao.mirasalon.core.domain.model.chat.ChatSession
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import kotlinx.coroutines.flow.Flow

interface StreamChatManager {
    fun getChannels(): Flow<List<ChatSession>>
    fun watchChannel(type: String, id: String): Flow<List<ChatMessage>>
    fun sendMessage(type: String, id: String, text: String, asUserId: String?): Flow<Outcome<Unit>>
    fun markRead(type: String, id: String): Flow<Outcome<Unit>>

    /**
     * Sends an image attachment (already uploaded) to a channel, with an optional caption.
     * Text and image are the only supported message types.
     */
    fun sendImageMessage(
        type: String,
        id: String,
        imageUrl: String,
        caption: String?,
        asUserId: String?
    ): Flow<Outcome<Unit>>
}
