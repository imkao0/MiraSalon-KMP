package iz.mkao.mirasalon.feature.chat.domain.repository

import iz.mkao.mirasalon.feature.chat.domain.model.ChatItem
import iz.mkao.mirasalon.feature.chat.domain.model.ChatMessage
import iz.mkao.mirasalon.feature.chat.domain.model.Conversation
import iz.mkao.mirasalon.feature.chat.domain.model.QuickAccessContact
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(conversationId: String, text: String): Result<Unit>
    suspend fun markAsRead(conversationId: String)
    suspend fun deleteHistory()

    suspend fun getCurrentUserAvatarUrl(): String?
    suspend fun getCurrentUserName(): String?

    /** Stable unique id of the signed-in user (server user UUID), used to tell our own
     *  messages apart from the chat partner's messages. */
    suspend fun getCurrentUserId(): String?
    suspend fun getChats(): List<ChatItem>
    suspend fun getQuickAccessContacts(): List<QuickAccessContact>
}
