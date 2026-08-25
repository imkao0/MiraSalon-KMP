package iz.mkao.mirasalon.feature.chat.domain.model

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val timestampEpochSeconds: Long,
    val status: MessageStatus = MessageStatus.SENT
)

data class Conversation(
    val id: String,
    val participantName: String,
    val participantRole: String? = null,
    val participantImageUrl: String?,
    val lastMessage: ChatMessage?,
    val unreadCount: Int = 0,
    val participantIds: List<String> = emptyList()
)
