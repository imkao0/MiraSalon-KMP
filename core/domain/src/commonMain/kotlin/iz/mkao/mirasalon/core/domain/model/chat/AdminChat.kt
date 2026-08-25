package iz.mkao.mirasalon.core.domain.model.chat

import kotlinx.serialization.Serializable

/** A single chat message exchanged with a customer. */
@Serializable
data class ChatMessage(
    val id: String,
    val sessionId: String = "",
    val senderId: String = "",
    val senderRole: String = "CLIENT", // CLIENT, SPECIALIST, ADMIN
    val actingAsId: String? = null,
    val text: String = "",
    val timestamp: Long = 0L,
    val isFromAdmin: Boolean = false,
    val status: String = "SENT", // SENT, DELIVERED, READ
    val isInternal: Boolean = false,
    val content: MessageContent = MessageContent.Text(text),
    val timeFormatted: String = ""
)

@Serializable
sealed class MessageContent {
    @Serializable
    data class Text(val text: String) : MessageContent()
    @Serializable
    data class Image(val url: String, val caption: String? = null) : MessageContent()
}

/** A support/chat conversation between the salon and one customer. */
@Serializable
data class ChatSession(
    val id: String,
    val customerId: String = "",
    val customerName: String = "",
    val specialistId: String? = null,
    val specialistName: String? = null,
    val memberIds: List<String> = emptyList(),
    val lastMessage: ChatMessage? = null,
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val isActive: Boolean = true,
    val participantName: String = customerName,
    val participantRole: String? = null,
    val participantAvatarUrl: String? = null,
    val participantId: String = customerId
)
