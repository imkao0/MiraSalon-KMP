package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.domain.model.AdminSpecialist
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistBreak
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistShift
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistStats
import iz.mkao.mirasalon.core.network.model.dto.SpecialistDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistPerformanceDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistShiftDto
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.SpecialistBreakDto

fun SpecialistDto.toAdminSpecialist(): AdminSpecialist = AdminSpecialist(
    id = id,
    salonId = salonId,
    name = name,
    role = role,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    rating = rating,
    isAvailable = isOnline || status.equals("ONLINE", ignoreCase = true),
    isActive = isActive,
    bio = bio ?: "",
    customersServed = customersServed,
    yearsOfExperience = yearsOfExperience,
    services = services.map { it.toDomain() }
)

fun SpecialistShiftDto.toDomain(): AdminSpecialistShift = AdminSpecialistShift(
    id = id ?: "",
    specialistId = specialistId ?: "",
    dayOfWeek = dayOfWeek,
    startTime = startTime,
    endTime = endTime,
    isActive = isWorkingDay,
    breaks = breaks.map { it.toDomain() }
)

fun SpecialistBreakDto.toDomain() = AdminSpecialistBreak(
    id = id ?: "",
    startTime = startTime,
    endTime = endTime,
    title = title
)

fun SpecialistPerformanceDto.toDomain(): AdminSpecialistStats {
    val completed = (appointmentCount * (completionRate / 100.0)).toInt()
    return AdminSpecialistStats(
        specialistId = specialistId,
        specialistName = name,
        totalAppointments = appointmentCount,
        completedAppointments = completed,
        cancelledAppointments = appointmentCount - completed,
        revenue = revenue,
        revenueGrowth = revenueGrowth,
        totalRevenue = revenue
    )
}

fun AdminSpecialist.toDto(): SpecialistDto = SpecialistDto(
    id = id,
    name = name,
    role = role,
    imageUrl = imageUrl,
    rating = rating,
    isAvailable = isAvailable,
    isActive = isActive,
    bio = bio,
    customersServed = customersServed,
    salonId = salonId
)

fun AdminSpecialistShift.toDto(): SpecialistShiftDto = SpecialistShiftDto(
    id = id,
    specialistId = specialistId,
    dayOfWeek = dayOfWeek,
    startTime = startTime,
    endTime = endTime,
    isWorkingDay = isActive,
    breaks = breaks.map { it.toDto() }
)

fun AdminSpecialistBreak.toDto() = SpecialistBreakDto(
    id = id,
    startTime = startTime,
    endTime = endTime,
    title = title
)
