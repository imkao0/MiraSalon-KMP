package iz.mkao.mirasalon.feature.chat.data.repository

import iz.mkao.mirasalon.core.domain.repository.UnreadMessagesSource
import iz.mkao.mirasalon.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UnreadMessagesSourceImpl(
    private val chatRepository: ChatRepository
) : UnreadMessagesSource {
    override fun observeUnreadMessagesCount(): Flow<Int> {
        return chatRepository.observeConversations().map { conversations ->
            conversations.sumOf { it.unreadCount }
        }
    }
}
