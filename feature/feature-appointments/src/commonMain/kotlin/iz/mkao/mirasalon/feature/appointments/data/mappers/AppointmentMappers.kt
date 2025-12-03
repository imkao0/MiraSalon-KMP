package iz.mkao.mirasalon.feature.appointments.data.mappers

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.network.model.dto.AppointmentDto
import iz.mkao.mirasalon.core.network.model.dto.AppointmentStatusDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceItemDto
import iz.mkao.mirasalon.feature.appointments.domain.model.Appointment
import iz.mkao.mirasalon.feature.appointments.domain.model.AppointmentStatus

fun AppointmentDto.toDomain(): Appointment {
    return Appointment(
        id = id,
        salonName = salonName,
        specialistName = specialistName,
        specialistId = specialistId,
        dateTime = dateTime,
        services = services.map { it.toDomain() },
        totalAmount = totalAmount,
        status = when (status) {
            AppointmentStatusDto.CONFIRMED -> AppointmentStatus.Confirmed
            AppointmentStatusDto.COMPLETED -> AppointmentStatus.Completed
            AppointmentStatusDto.CANCELLED -> AppointmentStatus.Cancelled
        }
    )
}

fun ServiceItemDto.toDomain(): Service {
    return Service(
        id = id,
        name = name,
        durationMinutes = durationMinutes,
        price = price
    )
}
