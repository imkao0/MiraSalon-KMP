package iz.mkao.mirasalon.feature.notifications.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.network.model.dto.NotificationDto
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.network.result.safeApiCall

class NotificationApi(private val httpClient: HttpClient) {

    suspend fun fetchNotifications(type: String? = null): NetworkResult<List<NotificationDto>> = safeApiCall {
        httpClient.get(Endpoints.NOTIFICATIONS) {
            type?.let { parameter("type", it) }
        }
    }

    suspend fun markAsRead(id: String): NetworkResult<Unit> = safeApiCall {
        httpClient.post(Endpoints.markAsRead(id))
    }

    suspend fun clearAll(): NetworkResult<Unit> = safeApiCall {
        httpClient.delete(Endpoints.NOTIFICATIONS)
    }

    suspend fun notifyChatReply(userId: String, senderName: String, conversationId: String): NetworkResult<Unit> = safeApiCall {
        httpClient.post(Endpoints.NOTIFY_REPLY) {
            setBody(mapOf(
                "userId" to userId,
                "senderName" to senderName,
                "conversationId" to conversationId
            ))
            contentType(ContentType.Application.Json)
        }
    }

    private object Endpoints {
        const val NOTIFICATIONS = "/v1/api/notifications"
        fun markAsRead(id: String) = "/v1/api/notifications/$id/read"
        const val NOTIFY_REPLY = "/v1/api/stream/notify-reply"
    }
}