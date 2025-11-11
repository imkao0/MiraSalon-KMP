package iz.mkao.mirasalon.server.service

import io.getstream.chat.java.models.Channel
import io.getstream.chat.java.models.Message
import io.getstream.chat.java.models.User
import io.getstream.chat.java.services.framework.Client as StreamSDKClient
import io.getstream.chat.java.services.framework.DefaultClient
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import iz.mkao.mirasalon.core.domain.model.UserRole

class StreamSyncService(
    private val apiKey: String,
    private val apiSecret: String
) {
    private val logger = LoggerFactory.getLogger(StreamSyncService::class.java)
    private var client: StreamSDKClient? = null

    init {
        if (apiKey.isNotBlank() && apiSecret.isNotBlank()) {
            val props = Properties()
            props.setProperty(DefaultClient.API_KEY_PROP_NAME, apiKey)
            props.setProperty(DefaultClient.API_SECRET_PROP_NAME, apiSecret)
            client = DefaultClient(props)
        } else {
            logger.warn("Stream API Key or Secret is missing. User synchronization will be disabled.")
        }
    }

    suspend fun syncUser(userId: String, name: String, role: UserRole, avatarUrl: String? = null) {
        val streamClient = client ?: run {
            logger.warn("⚠️ Stream client is NULL. Sync aborted for user {}", userId)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val streamRole = when (role) {
                    UserRole.ADMIN -> "admin"
                    else -> "user"
                }

                val userRequest = User.UserRequestObject.builder()
                    .id(userId)
                    .name(name)
                    .role(streamRole)
                    .additionalFields(mapOf("image" to (avatarUrl ?: "")))
                    .build()

                User.upsert()
                    .user(userRequest)
                    .withClient(streamClient)
                    .request()

                logger.info("✅ Successfully synced user {} to Stream", userId)
            } catch (e: Exception) {
                logger.error("💥 Exception syncing user {} to Stream: {}", userId, e.message)
                throw e
            }
        }
    }

    /**
     * Mirrors a chat message (received over the real-time WebSocket partition) into the
     * backing Stream channel so that the desktop admin app — which reads conversation
     * history from Stream — sees messages that originate from the mobile apps.
     *
     * The channel is keyed by the deterministic chat id ([channelId]) and is created (on
     * first use) with BOTH participants as members and the real customer as `createdBy`,
     * plus explicit `customerId`/`specialistId`/`customerName` custom fields. This is what
     * lets the desktop resolve the correct customer for the customer card instead of an
     * unrelated auto-generated creator.
     */
    suspend fun mirrorChatMessage(
        channelId: String,
        customerId: String,
        customerName: String,
        customerAvatarUrl: String?,
        specialistId: String,
        specialistName: String,
        senderId: String,
        text: String,
        messageId: String? = null
    ) {
        val streamClient = client ?: run {
            logger.warn("⚠️ Stream client is NULL. Chat mirror aborted for channel {}", channelId)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val creatorId = customerId.takeIf { it.isNotBlank() } ?: "admin"
                val channelData = Channel.ChannelRequestObject.builder()
                    .createdBy(User.UserRequestObject.builder().id(creatorId).name(customerName).build())
                    .members(
                        listOf(
                            Channel.ChannelMemberRequestObject.builder().userId(customerId).build(),
                            Channel.ChannelMemberRequestObject.builder().userId(specialistId).build()
                        )
                    )
                    .additionalField("customerId", customerId)
                    .additionalField("customerName", customerName)
                    .additionalField("customerAvatarUrl", customerAvatarUrl ?: "")
                    .additionalField("specialistId", specialistId)
                    .additionalField("specialistName", specialistName)
                    .build()

                Channel.getOrCreate("messaging", channelId)
                    .data(channelData)
                    .withClient(streamClient)
                    .request()

                val messageRequest = Message.MessageRequestObject.builder()
                    .text(text)
                    .userId(senderId)
                    .apply {
                        if (messageId != null) id(messageId)
                    }
                    .build()

                Message.send("messaging", channelId)
                    .message(messageRequest)
                    .withClient(streamClient)
                    .request()

                logger.info("✅ Mirrored chat message to Stream channel {} (sender={}, id={})", channelId, senderId, messageId)
            } catch (e: Exception) {
                logger.error("💥 Failed to mirror chat message to Stream channel {}: {}", channelId, e.message)
            }
        }
    }
}
