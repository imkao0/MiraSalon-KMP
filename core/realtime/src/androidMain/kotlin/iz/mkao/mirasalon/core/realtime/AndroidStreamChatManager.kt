package iz.mkao.mirasalon.core.realtime

import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.api.models.QueryChannelRequest
import io.getstream.chat.android.client.api.models.QueryChannelsRequest
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.Message
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.model.chat.ChatMessage
import iz.mkao.mirasalon.core.domain.model.chat.ChatSession
import iz.mkao.mirasalon.core.domain.model.chat.MessageContent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.StreamChatManager
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Date
import kotlin.coroutines.resume

class AndroidStreamChatManager(
    private val tokenProvider: SalonTokenProvider
) : StreamChatManager {

    private suspend fun getClient(): ChatClient? {
        if (!ChatClient.isInitialized) {
            // Wait for up to 5 seconds for Stream ChatClient to be initialized by the Gateway
            repeat(10) {
                if (ChatClient.isInitialized) return ChatClient.instance()
                delay(500)
            }
            return null
        }
        return ChatClient.instance()
    }

    override fun getChannels(): Flow<List<ChatSession>> = flow {
        val client = getClient() ?: return@flow emit(emptyList())
        val userId = tokenProvider.userId() ?: return@flow emit(emptyList())
        
        val filter = Filters.and(
            Filters.eq("type", "messaging"),
            Filters.`in`("members", listOf(userId))
        )
        val request = QueryChannelsRequest(filter = filter, limit = 30, messageLimit = 1)

        val result = suspendCancellableCoroutine { continuation ->
            client.queryChannels(request).enqueue { result ->
                continuation.resume(result)
            }
        }

        if (result.isSuccess) {
            emit(result.getOrNull()?.map { it.toChatSession(userId) } ?: emptyList())
        } else {
            emit(emptyList())
        }
    }

    override fun watchChannel(type: String, id: String): Flow<List<ChatMessage>> = flow {
        val client = getClient() ?: return@flow emit(emptyList())
        val request = QueryChannelRequest().withMessages(limit = 30)
        
        val result = suspendCancellableCoroutine { continuation ->
            client.channel(type, id).query(request).enqueue { result ->
                continuation.resume(result)
            }
        }
        
        if (result.isSuccess) {
            emit(result.getOrNull()?.messages?.map { it.toChatMessage(id) } ?: emptyList())
        } else {
            emit(emptyList())
        }
    }

    override fun sendMessage(type: String, id: String, text: String, asUserId: String?): Flow<Outcome<Unit>> = flow {
        val client = getClient() ?: run {
            emit(Outcome.Error(Failure.ServerError(0, "Chat client not initialized")))
            return@flow
        }
        val result = suspendCancellableCoroutine { continuation ->
            client.channel(type, id).sendMessage(Message(text = text)).enqueue { result ->
                continuation.resume(result)
            }
        }
        if (result.isSuccess) emit(Outcome.Success(Unit)) 
        else emit(Outcome.Error(Failure.ServerError(0, result.errorOrNull()?.message ?: "Unknown error")))
    }

    override fun markRead(type: String, id: String): Flow<Outcome<Unit>> = flow {
        val client = getClient()
        client?.channel(type, id)?.markRead()?.enqueue()
        emit(Outcome.Success(Unit))
    }

    override fun sendImageMessage(type: String, id: String, imageUrl: String, caption: String?, asUserId: String?): Flow<Outcome<Unit>> = flow {
        emit(Outcome.Success(Unit))
    }

    private fun Channel.toChatSession(currentUserId: String): ChatSession {
        val participants = ChatUtils.parseParticipantIds(id)
        val specialistId = extraData["specialistId"] as? String 
            ?: participants?.firstOrNull { it.startsWith("spec-") }
        
        val customerId = extraData["customerId"] as? String 
            ?: participants?.firstOrNull { it != specialistId }
            ?: currentUserId

        val lastMsg = messages.lastOrNull()

        return ChatSession(
            id = id,
            customerId = customerId,
            customerName = extraData["customerName"] as? String ?: "Customer",
            specialistId = specialistId,
            specialistName = extraData["specialistName"] as? String ?: "Specialist",
            memberIds = members.map { it.getUserId() },
            lastMessage = lastMsg?.toChatMessage(id),
            lastMessageTime = lastMessageAt?.time ?: 0L,
            unreadCount = unreadCount ?: 0,
            participantName = if (currentUserId == customerId) (extraData["specialistName"] as? String ?: "Specialist") else (extraData["customerName"] as? String ?: "Customer"),
            participantRole = if (currentUserId == customerId) (extraData["specialistRole"] as? String) else null,
            participantAvatarUrl = if (currentUserId == customerId) (extraData["specialistAvatarUrl"] as? String) else (extraData["customerAvatarUrl"] as? String),
            participantId = if (currentUserId == customerId) (specialistId ?: "") else customerId
        )
    }

    private fun Message.toChatMessage(sessionId: String): ChatMessage {
        val systemTZ = TimeZone.currentSystemDefault()
        val date = createdAt ?: Date()
        val dateTime = Instant.fromEpochMilliseconds(date.time).toLocalDateTime(systemTZ)
        val timeFormatted = "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"

        return ChatMessage(
            id = id,
            sessionId = sessionId,
            senderId = user.id,
            text = text,
            timestamp = date.time,
            isFromAdmin = user.role == "admin",
            content = MessageContent.Text(text),
            timeFormatted = timeFormatted
        )
    }
}
