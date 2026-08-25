package iz.mkao.mirasalon.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.Url
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DesktopNotificationService(
    private val client: HttpClient,
    private val scope: CoroutineScope
) {
    private val _notifications = MutableSharedFlow<String>()
    val notifications: SharedFlow<String> = _notifications.asSharedFlow()
    
    private val _events = MutableSharedFlow<DomainEvent>()
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    private var job: Job? = null

    init {
        connect()
    }

    private fun connect() {
        job?.cancel()
        job = scope.launch {
            val host = Url(ApiEndpoints.baseUrl()).host
            while (isActive) {
                try {
                    client.webSocket(host = host, port = 8080, path = ApiEndpoints.WebSocket.NOTIFICATIONS) {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                
                                runCatching {
                                    val event = DomainEventCodec.decode(text)
                                    _events.emit(event)
                                    
                                    // Extract user-friendly message for snackbars
                                    val message = when (event) {
                                        is DomainEvent.InventoryUpdated -> event.message
                                        is DomainEvent.OrderCreated -> "New Order: ${event.orderId} ($${event.totalAmount})"
                                        is DomainEvent.BookingCreated -> event.message
                                        is DomainEvent.ReviewSubmitted -> "New review: ${event.rating} stars"
                                        is DomainEvent.SpecialistStatusChanged -> event.message
                                        is DomainEvent.PromotionChanged -> event.message
                                        else -> null
                                    }
                                    message?.let { _notifications.emit(it) }
                                }.onFailure {
                                    // Fallback for non-JSON or legacy messages
                                    _notifications.emit(text)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    delay(5000.milliseconds)
                }
            }
        }
    }

    fun disconnect() {
        job?.cancel()
    }
}
