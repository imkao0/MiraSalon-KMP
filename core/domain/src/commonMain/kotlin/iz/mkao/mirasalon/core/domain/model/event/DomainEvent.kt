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
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class OrderCreated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val orderId: String,
        val totalAmount: Double = 0.0
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class OrderUpdated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val orderId: String,
        val status: String
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class BookingCreated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val bookingId: String,
        val specialistId: String = "",
        val specialistName: String? = null,
        val specialistAvatarUrl: String? = null,
        val customerName: String? = null,
        val customerAvatarUrl: String? = null,
        val serviceName: String? = null,
        val startTime: Long = 0L,
        val appointmentId: String = ""
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class BookingUpdated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val bookingId: String,
        val status: String,
        val specialistName: String? = null,
        val specialistAvatarUrl: String? = null,
        val customerName: String? = null,
        val customerAvatarUrl: String? = null,
        val appointmentId: String = ""
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class ReviewSubmitted(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val reviewId: String,
        val targetId: String = "",
        val targetType: String = "",
        val rating: Int = 0,
        val userName: String? = null,
        val userAvatarUrl: String? = null
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class ProductChanged(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val productId: String
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class InventoryUpdated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val productId: String,
        val newStock: Int,
        val newStockQuantity: Int = 0 // Some places might use this name
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class SpecialistStatusChanged(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val specialistId: String,
        val isAvailable: Boolean,
        val status: String = ""
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class SpecialistCreated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val specialistId: String
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class UserProfileUpdated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val userId: String,
        val userName: String? = null,
        val userAvatarUrl: String? = null
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class ServiceUpdated(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val serviceId: String
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class ChatMessageReceived(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val conversationId: String,
        val messageId: String,
        val senderId: String,
        val text: String,
        val senderName: String? = null,
        val senderAvatarUrl: String? = null,
        val specialistId: String? = null, // Specialist ID for server-side participant resolution
        val senderRole: String = "CLIENT", // CLIENT, SPECIALIST, ADMIN
        val actingAsId: String? = null, // For Admin impersonation
        val status: String = "SENT", // SENT, DELIVERED, READ
        val isInternal: Boolean = false // Zendesk-style internal notes
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class ChatSeen(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String = "Message seen",
        val conversationId: String,
        val userId: String
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class ChatTyping(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String = "Typing...",
        val conversationId: String,
        val userId: String,
        val isTyping: Boolean
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class NotificationReceived(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String? = null,
        override val message: String,
        val notificationType: String,
        val referenceId: String? = null,
        val senderName: String? = null,
        val senderAvatarUrl: String? = null,
        val messageId: String? = null
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class AppointmentReminder(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String? = null,
        override val message: String,
        val appointmentId: String,
        val appointmentTime: Long,
        val reminderType: String, // "30_MINUTES", "1_HOUR", "1_DAY", etc.
        val specialistName: String? = null,
        val specialistAvatarUrl: String? = null
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class PromotionChanged(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String?,
        override val message: String,
        val promotionId: String? = null,
        val promoTitle: String? = null
    ) : DomainEvent() {
        companion object
    }

    @Serializable
    data class ChatHistory(
        override val eventId: String,
        override val timestamp: Long,
        override val actorId: String? = null,
        override val message: String = "Chat history",
        val conversationId: String,
        val messages: List<ChatMessageReceived>
    ) : DomainEvent() {
        companion object
    }
}
