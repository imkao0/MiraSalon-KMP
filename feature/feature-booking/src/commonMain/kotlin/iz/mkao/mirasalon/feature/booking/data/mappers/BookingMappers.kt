package iz.mkao.mirasalon.feature.booking.data.mappers

import iz.mkao.mirasalon.core.database.dao.BookingWithServices
import iz.mkao.mirasalon.core.database.entity.BookingEntity
import iz.mkao.mirasalon.core.database.entity.BookingServiceEntity
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.AppointmentDto
import iz.mkao.mirasalon.core.network.model.dto.AppointmentStatusDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceItemDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistDto
import iz.mkao.mirasalon.core.network.model.dto.TimeSlotDto
import iz.mkao.mirasalon.feature.booking.domain.model.BookingSpecialist
import iz.mkao.mirasalon.feature.booking.domain.model.BookingStatus
import iz.mkao.mirasalon.feature.booking.domain.model.BookingTimeSlot
import iz.mkao.mirasalon.feature.booking.domain.model.ConfirmedBooking
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun TimeSlotDto.toDomain(): BookingTimeSlot = BookingTimeSlot(
    startTime = startTime,
    endTime = endTime,
    formattedTime = startTime.toTimeSlotLabel(),
    isAvailable = isAvailable
)

fun SpecialistDto.toBookingSpecialist(): BookingSpecialist = BookingSpecialist(
    id = id,
    name = name,
    role = role,
    salonId = salonId,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    rating = rating
)

fun ServiceDto.toDomain(): Service = Service(
    id = id,
    name = name,
    description = description ?: "",
    durationMinutes = durationMinutes,
    price = price,
    discountPercent = discountPercent,
    categoryId = categoryId ?: ""
)

fun ServiceItemDto.toDomainService(): Service = Service(
    id = id,
    name = name,
    durationMinutes = durationMinutes,
    price = price,
    discountPercent = discountPercent,
    imageUrl = null
)

fun AppointmentStatusDto.toBookingStatus(): BookingStatus = when (this) {
    AppointmentStatusDto.CONFIRMED -> BookingStatus.Confirmed
    AppointmentStatusDto.COMPLETED -> BookingStatus.Completed
    AppointmentStatusDto.CANCELLED -> BookingStatus.Cancelled
}

fun AppointmentDto.toConfirmedBooking(): ConfirmedBooking = ConfirmedBooking(
    id = id,
    salonName = salonName,
    specialistName = specialistName,
    specialistId = specialistId,
    dateTime = dateTime,
    services = services.map { it.toDomainService() },
    subtotalAmount = subtotalAmount,
    taxRatePercent = taxRatePercent,
    taxAmount = taxAmount,
    discountAmount = discountAmount,
    totalAmount = totalAmount,
    status = status.toBookingStatus(),
    customerName = userName.orEmpty(),
    customerEmail = userEmail.orEmpty(),
    customerPhone = "",
    salonAddress = salonAddress.orEmpty(),
    timeSlotLabel = dateTime.toTimeSlotLabel(),
    salonImageUrl = ApiEndpoints.resolveImageUrl(salonImageUrl),
    specialistImageUrl = ApiEndpoints.resolveImageUrl(specialistAvatarUrl),
    serviceImageUrl = null,
    reminderEnabled = reminderEnabled,
    isReviewed = isReviewed
)

fun AppointmentDto.toEntity() = BookingEntity(
    id = id,
    userId = userId,
    userName = userName,
    userEmail = userEmail,
    salonId = salonId,
    salonName = salonName,
    salonAddress = salonAddress,
    salonImageUrl = salonImageUrl,
    specialistId = specialistId,
    specialistName = specialistName,
    specialistAvatarUrl = specialistAvatarUrl,
    status = status.name,
    dateTime = dateTime,
    durationMinutes = durationMinutes,
    subtotalAmount = subtotalAmount,
    taxAmount = taxAmount,
    discountAmount = discountAmount,
    totalAmount = totalAmount,
    reminderEnabled = reminderEnabled,
    isReviewed = isReviewed
)

fun ServiceItemDto.toEntity(bookingId: String) = BookingServiceEntity(
    bookingId = bookingId,
    serviceId = id,
    name = name,
    price = price
)

fun BookingWithServices.toDomain(): ConfirmedBooking {
    val b = booking
    return ConfirmedBooking(
        id = b.id,
        salonName = b.salonName,
        specialistName = b.specialistName,
        specialistId = b.specialistId,
        dateTime = b.dateTime,
        services = services.map { Service(id = it.serviceId, name = it.name, durationMinutes = 0, price = it.price) },
        subtotalAmount = b.subtotalAmount,

        taxAmount = b.taxAmount,
        discountAmount = b.discountAmount,
        totalAmount = b.totalAmount,
        status = try { BookingStatus.valueOf(b.status.lowercase().capitalize()) } catch(e: Exception) { BookingStatus.Confirmed },
        customerName = b.userName.orEmpty(),
        customerEmail = b.userEmail.orEmpty(),
        customerPhone = "",
        salonAddress = b.salonAddress.orEmpty(),
        timeSlotLabel = b.dateTime.toTimeSlotLabel(),
        salonImageUrl = ApiEndpoints.resolveImageUrl(b.salonImageUrl),
        specialistImageUrl = ApiEndpoints.resolveImageUrl(b.specialistAvatarUrl),
        reminderEnabled = b.reminderEnabled,
        isReviewed = b.isReviewed
    )
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

internal fun Long.toTimeSlotLabel(): String {
    if (this <= 0L) return ""
    return try {
        val local = Instant.fromEpochMilliseconds(this)
            .toLocalDateTime(TimeZone.UTC)
        val hour12 = when {
            local.hour == 0 -> 12
            local.hour > 12 -> local.hour - 12
            else -> local.hour
        }
        val amPm = if (local.hour < 12) "AM" else "PM"
        "$hour12:${local.minute.toString().padStart(2, '0')} $amPm"
    } catch (e: Exception) {
        ""
    }
}
