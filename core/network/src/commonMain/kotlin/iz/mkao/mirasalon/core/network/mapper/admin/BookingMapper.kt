package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.domain.model.AdminAppointment
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStatus
import iz.mkao.mirasalon.core.network.model.dto.AppointmentDto

fun AppointmentDto.toDomain(): AdminAppointment = AdminAppointment(
    id = id,
    customerId = userId,
    customerName = userName ?: "Unknown",
    specialistId = specialistId,
    specialistName = specialistName,
    salonId = salonId,
    salonName = salonName,
    serviceIds = services.map { it.id },
    serviceNames = services.map { it.name },
    dateTime = dateTime,
    durationMinutes = durationMinutes,
    totalAmount = totalAmount,
    status = AdminAppointmentStatus.fromString(status.name),
    createdAt = createdAt
)
