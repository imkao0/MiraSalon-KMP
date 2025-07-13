package iz.mkao.mirasalon.core.realtime

import io.getstream.chat.java.models.Channel
import io.getstream.chat.java.models.Message
import io.getstream.chat.java.models.Sort
import io.getstream.chat.java.models.User
import io.getstream.chat.java.services.framework.DefaultClient
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.model.chat.ChatMessage
import iz.mkao.mirasalon.core.domain.model.chat.ChatSession
import iz.mkao.mirasalon.core.domain.model.chat.MessageContent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.StreamChatManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Properties
import io.getstream.chat.java.services.framework.Client as StreamSDKClient

class DesktopStreamChatManager(
    apiKey: String,
    apiSecret: String
) : StreamChatManager {

    private val client: StreamSDKClient? by lazy {
        if (apiKey.isNotBlank() && apiSecret.isNotBlank()) {
            val props = Properties()
            props.setProperty(DefaultClient.API_KEY_PROP_NAME, apiKey)
            props.setProperty(DefaultClient.API_SECRET_PROP_NAME, apiSecret)
            try {
                DefaultClient(props)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    override fun getChannels(): Flow<List<ChatSession>> = flow {
        val streamClient = client ?: run {
            emit(emptyList())
            return@flow
        }
        try {
            val sessions = withContext(Dispatchers.IO) {
                val request = Channel.list()
                    .filterConditions(
                        mapOf(
                            "type" to "messaging"
                        )
                    )
                    .watch(false)
                    .sorts(listOf(Sort.builder().field("last_message_at").direction(Sort.Direction.DESC).build()))

                val response = request.withClient(streamClient).request()
                response.channels?.map { it.toChatSession() } ?: emptyList()
            }
            emit(sessions)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun watchChannel(type: String, id: String): Flow<List<ChatMessage>> = flow {
        val streamClient = client ?: run {
            emit(emptyList())
            return@flow
        }
        try {
            val messages = withContext(Dispatchers.IO) {
                val creator = User.UserRequestObject.builder()
                    .id("admin")
                    .build()

                val request = Channel.getOrCreate(type, id)
                    .data(
                        Channel.ChannelRequestObject.builder()
                            .createdBy(creator)
                            .build()
                    )
                    .state(true)
                    .watch(false)

                val response = request.withClient(streamClient).request()
                response.messages?.map { it.toChatMessage(id) } ?: emptyList()
            }
            emit(messages)
        } catch (e: Exception) {
            Napier.e(e) { "[StreamChatManager] watchChannel failed for $id" }
            emit(emptyList())
        }
    }

    override fun sendMessage(
        type: String,
        id: String,
        text: String,
        asUserId: String?
    ): Flow<Outcome<Unit>> = flow {
        val streamClient = client ?: run {
            emit(Outcome.Error(Failure.ServerError(0, "Stream client not initialized")))
            return@flow
        }
        try {
            withContext(Dispatchers.IO) {
                val creatorId = asUserId ?: "admin"
                val creator = User.UserRequestObject.builder()
                    .id(creatorId)
                    .build()

                Channel.getOrCreate(type, id)
                    .data(
                        Channel.ChannelRequestObject.builder()
                            .createdBy(creator)
                            .build()
                    )
                    .withClient(streamClient)
                    .request()

                val messageRequest = Message.MessageRequestObject.builder()
                    .text(text)
                    .userId(asUserId ?: "admin")
                    .build()

                val request = Message.send(type, id)
                    .message(messageRequest)

                request.withClient(streamClient).request()
            }
            emit(Outcome.Success(Unit))
        } catch (e: Exception) {
            Napier.e(e) { "[StreamChatManager] sendMessage failed for $id" }
            emit(Outcome.Error(Failure.ServerError(0, e.message ?: "Unknown error sending message")))
        }
    }

    override fun sendImageMessage(
        type: String,
        id: String,
        imageUrl: String,
        caption: String?,
        asUserId: String?
    ): Flow<Outcome<Unit>> = flow {
        val streamClient = client ?: run {
            emit(Outcome.Error(Failure.ServerError(0, "Stream client not initialized")))
            return@flow
        }
        try {
            withContext(Dispatchers.IO) {
                Channel.getOrCreate(type, id)
                    .data(Channel.ChannelRequestObject.builder().build())
                    .withClient(streamClient)
                    .request()

                val attachment = Message.AttachmentRequestObject.builder()
                    .type("image")
                    .imageURL(imageUrl)
                    .fallback(caption ?: "Image")
                    .build()

                val messageRequest = Message.MessageRequestObject.builder()
                    .text(caption ?: "")
                    .userId(asUserId ?: "admin")
                    .attachment(attachment)
                    .build()

                val request = Message.send(type, id)
                    .message(messageRequest)

                request.withClient(streamClient).request()
            }
            emit(Outcome.Success(Unit))
        } catch (e: Exception) {
            Napier.e(e) { "[StreamChatManager] sendImageMessage failed for $id" }
            emit(Outcome.Error(Failure.ServerError(0, e.message ?: "Unknown error sending image message")))
        }
    }

    override fun markRead(type: String, id: String): Flow<Outcome<Unit>> = flow {
        val streamClient = client ?: run {
            emit(Outcome.Error(Failure.ServerError(0, "Stream client not initialized")))
            return@flow
        }
        try {
            withContext(Dispatchers.IO) {
                val request = Channel.markRead(type, id)
                    .userId("admin")
                request.withClient(streamClient).request()
            }
            emit(Outcome.Success(Unit))
        } catch (e: Exception) {
            emit(Outcome.Error(Failure.ServerError(0, e.message ?: "Unknown error marking as read")))
        }
    }

    private fun Channel.ChannelGetResponse.toChatSession(): ChatSession {
        val chan = channel ?: throw IllegalStateException("Channel is null in GetResponse")
        val lastMsg = messages?.lastOrNull()
        val participants = ChatUtils.parseParticipantIds(chan.id)
        
        val customerId = chan.additionalFields?.get("customerId") as? String 
            ?: participants?.firstOrNull { !it.contains("admin") && !it.startsWith("spec-") }
            ?: ""
            
        val specialistId = chan.additionalFields?.get("specialistId") as? String
            ?: participants?.firstOrNull { it.startsWith("spec-") }
            ?: "admin"

        return ChatSession(
            id = chan.id,
            customerId = customerId,
            customerName = chan.additionalFields?.get("customerName") as? String ?: "Customer",
            specialistId = specialistId,
            specialistName = chan.additionalFields?.get("specialistName") as? String ?: "Specialist",
            memberIds = chan.members?.map { it.userId ?: "" } ?: emptyList(),
            lastMessage = lastMsg?.toChatMessage(chan.id),
            lastMessageTime = chan.lastMessageAt?.time ?: 0L,
            unreadCount = read?.firstOrNull { it.user?.id == "admin" }?.unreadMessages ?: 0,
            participantName = chan.additionalFields?.get("customerName") as? String ?: "Customer",
            participantRole = null, // Admins don't need the customer's role usually
            participantAvatarUrl = chan.additionalFields?.get("customerAvatarUrl") as? String,
            participantId = customerId
        )
    }

    private fun Message.toChatMessage(sessionId: String): ChatMessage {
        return ChatMessage(
            id = id,
            sessionId = sessionId,
            senderId = user?.id ?: "",
            text = text ?: "",
            timestamp = createdAt?.time ?: 0L,
            isFromAdmin = user?.role == "admin" || user?.id == "admin",
            content = MessageContent.Text(text ?: ""),
            timeFormatted = "" // To be formatted by UI
        )
    }
}
