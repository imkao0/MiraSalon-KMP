package iz.mkao.mirasalon.server.domain.analytics

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.ActivityEventDto
import iz.mkao.mirasalon.core.network.model.dto.AppointmentDailyPoint
import iz.mkao.mirasalon.core.network.model.dto.AppointmentStatsDto
import iz.mkao.mirasalon.core.network.model.dto.AppointmentStatusDto
import iz.mkao.mirasalon.core.network.model.dto.OrderStatusDto
import iz.mkao.mirasalon.core.network.model.dto.PaymentStatsDto
import iz.mkao.mirasalon.core.network.model.dto.SalesTrendDto
import iz.mkao.mirasalon.core.network.model.dto.SalesTrendPoint
import iz.mkao.mirasalon.core.network.util.toPriceString
import iz.mkao.mirasalon.server.data.repository.AppointmentRepository
import iz.mkao.mirasalon.server.data.repository.AppointmentStatus
import iz.mkao.mirasalon.server.data.repository.OrderRepoStatus
import iz.mkao.mirasalon.server.data.repository.OrderRepository
import iz.mkao.mirasalon.server.data.repository.ProductRepository
import iz.mkao.mirasalon.server.data.repository.ServiceRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.util.ensureAdmin
import org.slf4j.LoggerFactory
import java.util.Calendar
import kotlin.math.abs

private val log = LoggerFactory.getLogger("AnalyticsRoutes")

fun Route.analyticsRoutes(
    appointmentRepository: AppointmentRepository,
    orderRepository: OrderRepository,
    specialistRepository: SpecialistRepository,
    serviceRepository: ServiceRepository,
    productRepository: ProductRepository,
    meterRegistry: MeterRegistry
) {

    authenticate("auth-jwt") {

        get("/appointments") {
            call.ensureAdmin()
            val timer = Timer.start(meterRegistry)
            val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 7
            try {
                val stats = calculateUpcomingAppointments(appointmentRepository, days)
                timer.stop(meterRegistry.timer("analytics_query_duration", "type", "appointments"))
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = stats))
            } catch (e: Exception) {
                log.error("Error calculating appointment stats", e)
                timer.stop(meterRegistry.timer("analytics_query_duration", "type", "appointments"))
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Failed to calculate appointment stats"))
            }
        }

        get("/overview") {
            call.ensureAdmin()
            val timer = Timer.start(meterRegistry)
            val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 7
            try {
                val stats = calculateDashboardOverview(appointmentRepository, orderRepository, days)
                timer.stop(meterRegistry.timer("analytics_query_duration", "type", "overview"))
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = stats))
            } catch (e: Exception) {
                log.error("Error calculating dashboard overview", e)
                timer.stop(meterRegistry.timer("analytics_query_duration", "type", "overview"))
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Failed to calculate dashboard overview"))
            }
        }

        get("/low-stock") {
            call.ensureAdmin()
            try {
                val threshold = call.request.queryParameters["threshold"]?.toIntOrNull() ?: 10
                val products = productRepository.findLowStock(threshold)
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = products))
            } catch (e: Exception) {
                log.error("Error fetching low stock products", e)
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Failed to fetch low stock products"))
            }
        }

        get("/specialists") {
            call.ensureAdmin()
            val timer = Timer.start(meterRegistry)
            val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 7
            try {
                val performance = calculateSpecialistPerformance(appointmentRepository, specialistRepository, days)
                timer.stop(meterRegistry.timer("analytics_query_duration", "type", "specialists"))
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = performance))
            } catch (e: Exception) {
                log.error("Error calculating specialist performance", e)
                timer.stop(meterRegistry.timer("analytics_query_duration", "type", "specialists"))
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Failed to calculate specialist performance"))
            }
        }

        get("/services") {
            call.ensureAdmin()
            val timer = Timer.start(meterRegistry)
            val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 7
            val popularity = calculateServicePopularity(appointmentRepository, serviceRepository, days)
            timer.stop(meterRegistry.timer("analytics_query_duration", "type", "services"))
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = popularity))
        }

        get("/payments") {
            call.ensureAdmin()
            val completedApps = appointmentRepository.countByStatus(AppointmentStatus.COMPLETED)
            // ...
            val confirmedApps = appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED)
            val cancelledApps = appointmentRepository.countByStatus(AppointmentStatus.CANCELLED)

            val completedOrders = orderRepository.countByStatusInRange(OrderRepoStatus.DELIVERED, 0, System.currentTimeMillis())
            val pendingOrders = orderRepository.countByStatusInRange(OrderRepoStatus.PENDING, 0, System.currentTimeMillis())
            val cancelledOrders = orderRepository.countByStatusInRange(OrderRepoStatus.CANCELLED, 0, System.currentTimeMillis())

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = PaymentStatsDto(
                        succeeded = completedApps + confirmedApps + completedOrders + pendingOrders,
                        refunded = 0,
                        failed = cancelledApps + cancelledOrders
                    )
                )
            )
        }

        get("/sales") {
            call.ensureAdmin()
            val timer = Timer.start(meterRegistry)
            val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 7
            try {
                val trend = calculateSalesTrend(appointmentRepository, orderRepository, days)
                timer.stop(meterRegistry.timer("analytics_query_duration", "type", "sales"))
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = trend))
            } catch (e: Exception) {
                log.error("Error calculating sales trend", e)
                timer.stop(meterRegistry.timer("analytics_query_duration", "type", "sales"))
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Failed to calculate sales trend"))
            }
        }

        get("/activity") {
            call.ensureAdmin()
            val timer = Timer.start(meterRegistry)
            try {
                val now = System.currentTimeMillis()
                // ...
                // Expand range to include recent past and upcoming appointments
                val start = now - (30L * 24 * 60 * 60 * 1000) // Last 30 days
                val end = now + (30L * 24 * 60 * 60 * 1000)   // Next 30 days

                val appointments = appointmentRepository.findByDateRange(start, end)
                val orders = orderRepository.findByDateRange(start, now)

                val appActivities = appointments.map { app ->
                    ActivityEventDto(
                        id = app.id,
                        type = "APPOINTMENT",
                        customerEmail = app.userName ?: "Unknown",
                        status = app.status.name,
                        timestamp = app.dateTime.toString(),
                        imageUrl = app.specialistAvatarUrl ?: app.specialistId,
                        serviceName = app.services.firstOrNull()?.name ?: "Service",
                        details = "Booking for ${app.services.joinToString { it.name }}"
                    )
                }

                val orderActivities = orders.map { order ->
                    ActivityEventDto(
                        id = order.id,
                        type = "STORE_ORDER",
                        customerEmail = order.userName ?: order.userId,
                        status = order.status.name,
                        timestamp = order.createdAt.toString(),
                        imageUrl = null,
                        serviceName = "Product Purchase",
                        details = "Bought ${order.items.size} item(s) for ${order.totalAmount.toPriceString()}"
                    )
                }

                // Sort by proximity to 'now' so that current/upcoming/recent activity is prioritized
                val allActivities = (appActivities + orderActivities)
                    .sortedBy { abs((it.timestamp.toLongOrNull() ?: 0L) - now) }
                    .take(40)

                timer.stop(meterRegistry.timer("analytics_query_duration", "type", "activity"))
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = allActivities
                    )
                )
            } catch (e: Exception) {
                log.error("Error calculating activity", e)
                timer.stop(meterRegistry.timer("analytics_query_duration", "type", "activity"))
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Failed to calculate activity"))
            }
        }
    }
}

private fun calculateUpcomingAppointments(
    appRepo: AppointmentRepository,
    days: Int
): AppointmentStatsDto {
    val now = System.currentTimeMillis()
    
    // For "Upcoming" stats, we look FORWARD from now
    val start = getDayStart(now)
    val end = start + (days.toLong() * 24 * 60 * 60 * 1000)
    
    // For growth calculation, we compare to the PREVIOUS period (last 'days' days)
    val prevStart = start - (days.toLong() * 24 * 60 * 60 * 1000)
    val prevEnd = start

    val appointments = appRepo.findByDateRange(start, end)
    val prevApps = appRepo.findByDateRange(prevStart, prevEnd)

    // "Upcoming" card only cares about volume, but we'll include revenue for completeness
    val appointmentRevenue = appointments.filter {
        it.status == AppointmentStatusDto.CONFIRMED ||
        it.status == AppointmentStatusDto.COMPLETED
    }.sumOf { it.totalAmount }

    val prevRevenue = prevApps.filter {
        it.status == AppointmentStatusDto.CONFIRMED ||
        it.status == AppointmentStatusDto.COMPLETED
    }.sumOf { it.totalAmount }

    val revenueGrowth = calculateGrowth(appointmentRevenue, prevRevenue)

    // Group by date
    val points = mutableListOf<AppointmentDailyPoint>()
    val calendar = Calendar.getInstance()

    for (i in 0 until days) {
        val currentDayMillis = start + (i.toLong() * 24 * 60 * 60 * 1000)
        calendar.timeInMillis = currentDayMillis
        val dateStr = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}"

        val dayStart = getDayStart(currentDayMillis)
        val dayEnd = dayStart + (24 * 60 * 60 * 1000)

        val dayApps = appointments.filter {
            val time = it.dateTime
            time in dayStart until dayEnd
        }

        val confirmed = dayApps.count {
            it.status == AppointmentStatusDto.CONFIRMED ||
            it.status == AppointmentStatusDto.COMPLETED
        }

        val cancelled = dayApps.count { it.status == AppointmentStatusDto.CANCELLED }

        // Logic for New vs Returning
        var newCount = 0
        var returningCount = 0

        val uniqueUsers = dayApps.mapNotNull { it.userId }.distinct()

        for (userId in uniqueUsers) {
            val isReturning = appRepo.hasAppointmentBefore(userId, dayStart)
            if (isReturning) returningCount++ else newCount++
        }

        points.add(AppointmentDailyPoint(
            date = dateStr,
            confirmed = confirmed,
            cancelled = cancelled,
            newClients = newCount,
            returningClients = returningCount
        ))
    }

    val totalConfirmedCount = appointments.count { 
        it.status == AppointmentStatusDto.CONFIRMED || it.status == AppointmentStatusDto.COMPLETED 
    }

    val totalCancelledCount = appointments.count { it.status == AppointmentStatusDto.CANCELLED }

    return AppointmentStatsDto(
        days = days,
        points = points,
        totalConfirmed = totalConfirmedCount,
        totalCancelled = totalCancelledCount,
        revenue = appointmentRevenue,
        appointmentRevenue = appointmentRevenue,
        productRevenue = 0.0,
        revenueGrowth = revenueGrowth
    )
}

private suspend fun calculateDashboardOverview(
    appRepo: AppointmentRepository,
    orderRepo: OrderRepository,
    days: Int
): AppointmentStatsDto {
    val now = System.currentTimeMillis()
    
    // Align to full days, including Today
    val end = getDayStart(now) + (24 * 60 * 60 * 1000)
    val start = getDayStart(now - (days.toLong() - 1) * 24 * 60 * 60 * 1000)
    val periodMillis = end - start
    
    val prevStart = start - periodMillis
    val prevEnd = start

    val appointments = appRepo.findByCreatedAtRange(start, end)
    val orders = orderRepo.findByDateRange(start, end)
    
    val prevApps = appRepo.findByCreatedAtRange(prevStart, prevEnd)
    val prevOrders = orderRepo.findByDateRange(prevStart, prevEnd)

    log.info("Dashboard Overview: Found ${appointments.size} appointments and ${orders.size} orders in range $start to $end")
    appointments.forEach { log.info("App Detail: id=${it.id}, status=${it.status}, amount=${it.totalAmount}, user=${it.userName}, created=${it.createdAt}") }

    val appointmentRevenue = appointments.filter {
        it.status == AppointmentStatusDto.CONFIRMED ||
        it.status == AppointmentStatusDto.COMPLETED
    }.sumOf { it.totalAmount }

    val productRevenue = orders.filter {
        it.status == OrderStatusDto.PENDING ||
        it.status == OrderStatusDto.SHIPPED ||
        it.status == OrderStatusDto.DELIVERED
    }.sumOf { it.totalAmount }

    val currentRevenue = appointmentRevenue + productRevenue

    val prevRevenue = prevApps.filter {
        it.status == AppointmentStatusDto.CONFIRMED ||
        it.status == AppointmentStatusDto.COMPLETED
    }.sumOf { it.totalAmount } + prevOrders.filter {
        it.status == OrderStatusDto.PENDING ||
        it.status == OrderStatusDto.SHIPPED ||
        it.status == OrderStatusDto.DELIVERED
    }.sumOf { it.totalAmount }

    val revenueGrowth = calculateGrowth(currentRevenue, prevRevenue)

    // Group by date
    val points = mutableListOf<AppointmentDailyPoint>()
    val calendar = java.util.Calendar.getInstance()

    for (i in 0 until days) {
        val currentDayMillis = start + (i.toLong() * 24 * 60 * 60 * 1000)
        calendar.timeInMillis = currentDayMillis
        val dateStr = "${calendar.get(java.util.Calendar.DAY_OF_MONTH)}/${calendar.get(java.util.Calendar.MONTH) + 1}"

        val dayStart = getDayStart(currentDayMillis)
        val dayEnd = dayStart + (24 * 60 * 60 * 1000)

        val dayApps = appointments.filter {
            val time = it.dateTime
            time in dayStart until dayEnd
        }

        val dayOrders = orders.filter { it.createdAt in dayStart until dayEnd }

        val confirmed = dayApps.count {
            it.status == AppointmentStatusDto.CONFIRMED ||
            it.status == AppointmentStatusDto.COMPLETED
        }

        val cancelled = dayApps.count { it.status == AppointmentStatusDto.CANCELLED }

        // Logic for New vs Returning
        var newCount = 0
        var returningCount = 0

        val uniqueUsers = (dayApps.mapNotNull { it.userId } + dayOrders.map { it.userId }).distinct()

        for (userId in uniqueUsers) {
            val isReturning = appRepo.hasAppointmentBefore(userId, dayStart) || orderRepo.hasOrderBefore(userId, dayStart)
            if (isReturning) returningCount++ else newCount++
        }

        points.add(AppointmentDailyPoint(
            date = dateStr,
            confirmed = confirmed,
            cancelled = cancelled,
            newClients = newCount,
            returningClients = returningCount
        ))
    }

    // Summary counts also reflect ONLY appointments
    val totalConfirmedCount = appointments.count { 
        it.status == AppointmentStatusDto.CONFIRMED || it.status == AppointmentStatusDto.COMPLETED 
    }

    val totalCancelledCount = appointments.count { it.status == AppointmentStatusDto.CANCELLED }

    return AppointmentStatsDto(
        days = days,
        points = points,
        totalConfirmed = totalConfirmedCount,
        totalCancelled = totalCancelledCount,
        revenue = currentRevenue,
        appointmentRevenue = appointmentRevenue,
        productRevenue = productRevenue,
        revenueGrowth = revenueGrowth
    )
}

private suspend fun calculateSpecialistPerformance(
    repository: AppointmentRepository,
    specialistRepo: SpecialistRepository,
    days: Int
): List<iz.mkao.mirasalon.core.network.model.dto.SpecialistPerformanceDto> {
    val end = System.currentTimeMillis()
    val start = end - (days.toLong() * 24 * 60 * 60 * 1000)
    val appointments = repository.findByDateRange(start, end)

    val allSpecialists = specialistRepo.findAll(salonId = null, page = 1, pageSize = 100)
    val specialistsMap = appointments.groupBy { it.specialistId }

    return allSpecialists.map { specialist ->
        val apps = specialistsMap[specialist.id] ?: emptyList()
        val completed = apps.count { it.status == AppointmentStatusDto.CONFIRMED || it.status == AppointmentStatusDto.COMPLETED }
        val total = apps.size

        val revenue = apps.filter { it.status == AppointmentStatusDto.CONFIRMED || it.status == AppointmentStatusDto.COMPLETED }.sumOf { it.totalAmount }
        val rate = if (total > 0) completed.toDouble() / total else 0.0

        // Dummy target: 10 appointments per week
        val target = (days / 7.0) * 10.0
        val achievement = (completed / target).coerceIn(0.0, 1.2)

        iz.mkao.mirasalon.core.network.model.dto.SpecialistPerformanceDto(
            specialistId = specialist.id,
            name = specialist.name,
            appointmentCount = completed,
            completionRate = rate,
            revenue = revenue,
            targetAchievement = achievement
        )
    }.sortedByDescending { it.revenue }
}

private suspend fun calculateServicePopularity(
    repository: AppointmentRepository,
    serviceRepo: ServiceRepository,
    days: Int
): List<iz.mkao.mirasalon.core.network.model.dto.ServicePopularityDto> {
    val end = System.currentTimeMillis()
    val start = end - (days.toLong() * 24 * 60 * 60 * 1000)
    val appointments = repository.findByDateRange(start, end)

    val allServices = serviceRepo.findAll(categoryId = null, page = 1, pageSize = 100)

    // Flat map to all services in these appointments
    val bookedServices = appointments.flatMap { app ->
        app.services.map { it.id }
    }.groupBy { it }

    val totalBookings = appointments.size.toDouble()

    return allServices.map { service ->
        val count = bookedServices[service.id]?.size ?: 0
        val ratio = if (totalBookings > 0) count / totalBookings else 0.0

        iz.mkao.mirasalon.core.network.model.dto.ServicePopularityDto(
            serviceId = service.id,
            name = service.name,
            count = count,
            ratio = ratio.coerceIn(0.0, 1.0)
        )
    }.sortedByDescending { it.count }.take(8)
}

private suspend fun calculateSalesTrend(
    appRepo: AppointmentRepository,
    orderRepo: OrderRepository,
    days: Int
): SalesTrendDto {
    val now = System.currentTimeMillis()
    
    // Align to full days, including Today
    val end = getDayStart(now) + (24 * 60 * 60 * 1000)
    val start = getDayStart(now - (days.toLong() - 1) * 24 * 60 * 60 * 1000)
    val periodMillis = end - start
    
    val prevStart = start - periodMillis
    val prevEnd = start

    val prevApps = appRepo.findByCreatedAtRange(prevStart, prevEnd)
    val prevOrders = orderRepo.findByDateRange(prevStart, prevEnd)

    val appointments = appRepo.findByCreatedAtRange(start, end)
    val orders = orderRepo.findByDateRange(start, end)

    val appRevenue = appointments.filter {
        it.status == AppointmentStatusDto.CONFIRMED ||
        it.status == AppointmentStatusDto.COMPLETED
    }.sumOf { it.totalAmount }

    val orderRevenue = orders.filter {
        it.status == OrderStatusDto.PENDING ||
        it.status == OrderStatusDto.SHIPPED ||
        it.status == OrderStatusDto.DELIVERED
    }.sumOf { it.totalAmount }

    val currentTotal = appRevenue + orderRevenue

    val prevTotal = prevApps.filter {
        it.status == AppointmentStatusDto.CONFIRMED ||
        it.status == AppointmentStatusDto.COMPLETED
    }.sumOf { it.totalAmount } + prevOrders.filter {
        it.status == OrderStatusDto.PENDING ||
        it.status == OrderStatusDto.SHIPPED ||
        it.status == OrderStatusDto.DELIVERED
    }.sumOf { it.totalAmount }

    val revenueGrowth = calculateGrowth(currentTotal, prevTotal)

    val points = mutableListOf<SalesTrendPoint>()
    val calendar = java.util.Calendar.getInstance()

    for (i in 0 until days) {
        // Points should go from start to end (past to now)
        val currentDayMillis = start + (i.toLong() * 24 * 60 * 60 * 1000)
        calendar.timeInMillis = currentDayMillis
        val dateStr = "${calendar.get(java.util.Calendar.DAY_OF_MONTH)}/${calendar.get(java.util.Calendar.MONTH) + 1}"

        val dayStart = getDayStart(currentDayMillis)
        val dayEnd = dayStart + (24 * 60 * 60 * 1000)

        val dayApps = appointments.filter {
            val time = it.createdAt
            time in dayStart until dayEnd
        }
        val dayOrders = orders.filter { it.createdAt in dayStart until dayEnd }

        val appSales = dayApps.filter {
            it.status == AppointmentStatusDto.CONFIRMED ||
            it.status == AppointmentStatusDto.COMPLETED
        }.sumOf { it.totalAmount }

        val orderSales = dayOrders.filter {
            it.status == OrderStatusDto.PENDING ||
            it.status == OrderStatusDto.SHIPPED ||
            it.status == OrderStatusDto.DELIVERED
        }.sumOf { it.totalAmount }

        val confirmedAppointments = dayApps.count { 
            it.status == AppointmentStatusDto.CONFIRMED || it.status == AppointmentStatusDto.COMPLETED 
        }

        points.add(SalesTrendPoint(
            date = dateStr,
            sales = appSales + orderSales,
            appointments = confirmedAppointments
        ))
    }

    return SalesTrendDto(
        days = days,
        points = points,
        appointmentRevenue = appRevenue,
        productRevenue = orderRevenue,
        revenueGrowth = revenueGrowth
    )
}

private fun calculateGrowth(current: Double, previous: Double): Double {
    return if (previous == 0.0) {
        if (current > 0) 100.0 else 0.0
    } else {
        ((current - previous) / previous) * 100.0
    }
}

private fun getDayStart(millis: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
