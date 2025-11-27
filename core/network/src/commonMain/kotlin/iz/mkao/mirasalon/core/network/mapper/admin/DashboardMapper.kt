package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.domain.model.*
import iz.mkao.mirasalon.core.network.model.dto.*
import iz.mkao.mirasalon.core.network.model.dto.AppointmentDailyPoint as AppointmentDailyPointDto
import iz.mkao.mirasalon.core.network.config.ApiEndpoints

fun AppointmentStatsDto.toDomain(): AdminAppointmentStats = AdminAppointmentStats(
    total = totalConfirmed + totalCancelled,
    confirmed = totalConfirmed,
    completed = totalConfirmed, 
    cancelled = totalCancelled,
    revenue = revenue,
    appointmentRevenue = appointmentRevenue,
    productRevenue = productRevenue,
    revenueGrowth = revenueGrowth,
    points = points.map { it.toDomain() }
)

fun AppointmentDailyPointDto.toDomain(): AppointmentDailyPoint =
    AppointmentDailyPoint(
        date = date,
        returningClients = returningClients,
        newClients = newClients,
        confirmed = confirmed,
        cancelled = cancelled
    )

fun SalesTrendDto.toDomain(): SalesTrend = SalesTrend(
    points = points.map { it.toDomain() },
    totalRevenue = points.sumOf { it.sales },
    appointmentRevenue = appointmentRevenue,
    productRevenue = productRevenue,
    revenueGrowth = revenueGrowth
)

fun SalesTrendPoint.toDomain(): SalesDataPoint = SalesDataPoint(
    date = date,
    amount = sales,
    appointments = appointments
)

fun ActivityEventDto.toDomain(): ActivityEvent = ActivityEvent(
    id = id,
    type = type,
    message = details,
    timestamp = timestamp.toLongOrNull() ?: 0L,
    customerEmail = customerEmail,
    status = status,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    serviceName = serviceName
)

fun SpecialistPerformanceDto.toPerformanceDomain(): SpecialistPerformance = SpecialistPerformance(
    specialistId = specialistId,
    specialistName = name,
    name = name,
    bookingCount = appointmentCount,
    revenue = revenue,
    targetAchievement = targetAchievement.toFloat()
)

fun ServicePopularityDto.toPopularityDomain(): ServicePopularity = ServicePopularity(
    serviceId = serviceId,
    serviceName = name,
    name = name,
    count = count,
    bookingCount = count,
    ratio = ratio.toFloat()
)
