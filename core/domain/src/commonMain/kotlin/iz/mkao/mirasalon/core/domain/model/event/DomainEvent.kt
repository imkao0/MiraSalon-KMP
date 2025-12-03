package iz.mkao.mirasalon.core.domain.model.event

import kotlinx.serialization.Serializable

@Serializable
sealed class DomainEvent {
    abstract val eventId: String
    abstract val timestamp: Long
    abstract val actorId: String?
    abstract val message: String

    @Serializable
    data class Connected(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String? = null,
        override val message: String = "Connected"
    ) : DomainEvent()

    @Serializable
    data class OrderCreated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val orderId: String,
        val totalAmount: Double = 0.0
    ) : DomainEvent()

    @Serializable
    data class OrderUpdated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val orderId: String,
        val status: String
    ) : DomainEvent()

    @Serializable
    data class BookingCreated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val bookingId: String,
        val specialistId: String = "",
        val startTime: Long = 0L,
        val appointmentId: String = ""
    ) : DomainEvent()

    @Serializable
    data class BookingUpdated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val bookingId: String,
        val status: String,
        val appointmentId: String = ""
    ) : DomainEvent()

    @Serializable
    data class ReviewSubmitted(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val reviewId: String,
        val targetId: String = "",
        val targetType: String = "",
        val rating: Int = 0,
        val userName: String? = null,
        val userAvatarUrl: String? = null
    ) : DomainEvent()

    @Serializable
    data class ProductChanged(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val productId: String
    ) : DomainEvent()

    @Serializable
    data class InventoryUpdated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val productId: String,
        val newStock: Int,
        val newStockQuantity: Int = 0 // Some places might use this name
    ) : DomainEvent()

    @Serializable
    data class SpecialistStatusChanged(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val specialistId: String,
        val isAvailable: Boolean,
        val status: String = ""
    ) : DomainEvent()

    @Serializable
    data class UserProfileUpdated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val userId: String
    ) : DomainEvent()

    @Serializable
    data class ServiceUpdated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val serviceId: String
    ) : DomainEvent()

    @Serializable
    data class ChatMessageReceived(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val conversationId: String,
        val messageId: String,
        val senderId: String,
        val text: String
    ) : DomainEvent()

    @Serializable
    data class ChatSeen(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String = "Message seen",
        val conversationId: String,
        val userId: String
    ) : DomainEvent()

    @Serializable
    data class NotificationReceived(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String? = null,
        override val message: String,
        val type: String,
        val referenceId: String? = null
    ) : DomainEvent()

    @Serializable
    data class AppointmentReminder(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String? = null,
        override val message: String,
        val appointmentId: String,
        val appointmentTime: Long,
        val reminderType: String // "30_MINUTES", "1_HOUR", "1_DAY", etc.
    ) : DomainEvent()

    @Serializable
    data class PromotionChanged(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String,
        override val message: String,
        val promotionId: String? = null
    ) : DomainEvent()
}
