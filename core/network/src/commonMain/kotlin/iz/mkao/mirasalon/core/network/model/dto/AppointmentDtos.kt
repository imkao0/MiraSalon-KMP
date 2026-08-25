package iz.mkao.mirasalon.core.network.model.dto

import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
enum class AppointmentStatusDto {
    CONFIRMED, COMPLETED, CANCELLED
}

@Serializable
data class ServiceItemDto(
    val id: String,
    val name: String,
    val price: Double,
    val discountPercent: Int = 0,
    val durationMinutes: Int
)

@Serializable
data class AppointmentDto(
    val id: String,
    val userId: String,
    val userName: String? = null,
    val userAvatarUrl: String? = null,
    val userEmail: String? = null,
    val salonId: String,
    val salonName: String,
    val salonAddress: String? = null,
    val salonImageUrl: String? = null,
    val specialistId: String,
    val specialistUserId: String? = null,
    val specialistName: String,
    val specialistAvatarUrl: String? = null,
    val specialistTitle: String? = null,
    val status: AppointmentStatusDto,
    val dateTime: Long, // Standardized to Long Epoch Millis
    val durationMinutes: Int,
    val services: List<ServiceItemDto>,
    val subtotalAmount: Double,
    val taxRatePercent: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val promoCode: String? = null,
    val totalAmount: Double,
    val reminderEnabled: Boolean = true,
    val isReviewed: Boolean = false,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
data class CreateAppointmentRequest(
    val salonId: String,
    val specialistId: String,
    val dateTime: Long, // Standardized to Long Epoch Millis
    val serviceIds: List<String>,
    val promoCode: String? = null
)

@Serializable
data class UpdateAppointmentStatusRequest(
    val status: String
)

@Serializable
data class CancelAppointmentResponse(
    val id: String,
    val cancelledAt: Long
)

@Serializable
data class SimpleMessageResponse(
    val message: String
)

@Serializable
data class UpdateReminderRequest(
    val enabled: Boolean
)
