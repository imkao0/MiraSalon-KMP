package iz.mkao.mirasalon.feature.booking.domain.model

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.Specialist

import iz.mkao.mirasalon.core.network.config.ApiEndpoints

/** A single bookable time slot for a specialist. */
data class BookingTimeSlot(
    val startTime: Long,
    val endTime: Long,
    val formattedTime: String,
    val isAvailable: Boolean
)

/** Confirmed booking data for receipts and lists. */
data class ConfirmedBooking(
    val id: String,
    val salonName: String,
    val specialistName: String,
    val specialistId: String,
    val dateTime: Long,
    val services: List<Service>,
    val subtotalAmount: Double = 0.0,
    val taxRatePercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double,
    val status: BookingStatus = BookingStatus.Confirmed,
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val salonAddress: String = "",
    val createdAt: Long = 0L,
    val timeSlotLabel: String = "",
    val reminderEnabled: Boolean = false,
    val isReviewed: Boolean = false,
    val salonImageUrl: String? = null,
    val specialistImageUrl: String? = null,
    val serviceImageUrl: String? = null
) {
    val qrPayload: String
        get() = buildString {
            append("MIRASALON-BOOKING")
            append("\nBooking ID: ").append(id.take(6))
            if (customerName.isNotBlank()) append("\nCustomer: ").append(customerName)
            if (customerPhone.isNotBlank()) append("\nPhone: ").append(customerPhone)
            append("\nSalon: ").append(salonName)
            append("\nSpecialist: ").append(specialistName)
            append("\nServices: ").append(services.joinToString(", ") { it.name })
            append("\nDate/Time: ").append(dateTime)
            append("\nTotal: ").append(totalAmount)
            append("\nStatus: ").append(status.name)
        }

    fun canCancel(currentTimeMillis: Long): Boolean {
        if (status != BookingStatus.Confirmed) return false
        val fortyEightHoursInMillis = 48L * 60 * 60 * 1000
        return (dateTime - currentTimeMillis) >= fortyEightHoursInMillis
    }
}

enum class BookingStatus {
    Confirmed, Completed, Cancelled
}

/** UI-facing specialist subset. */
data class BookingSpecialist(
    val id: String,
    val name: String,
    val role: String,
    val salonId: String,
    val imageUrl: String? = null,
    val rating: Double = 0.0
)

fun Specialist.toBookingSpecialist(): BookingSpecialist = BookingSpecialist(
    id = id,
    name = name,
    role = role,
    salonId = salonId,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    rating = rating
)
