package iz.mkao.mirasalon.server.domain.salon

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.LocalFileContent
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.DashboardDto
import iz.mkao.mirasalon.core.network.model.dto.SalonHomeDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateSalonRequest
import iz.mkao.mirasalon.server.data.repository.AppointmentRepository
import iz.mkao.mirasalon.server.data.repository.AppointmentStatus
import iz.mkao.mirasalon.server.data.repository.OrderRepoStatus
import iz.mkao.mirasalon.server.data.repository.OrderRepository
import iz.mkao.mirasalon.server.data.repository.PromotionRepository
import iz.mkao.mirasalon.server.data.repository.SalonFetchResult
import iz.mkao.mirasalon.server.data.repository.SalonRepository
import iz.mkao.mirasalon.server.data.repository.SalonUpdateResult
import iz.mkao.mirasalon.server.data.repository.ServiceRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.domain.promotion.toDto
import iz.mkao.mirasalon.server.util.AppConfig
import iz.mkao.mirasalon.server.util.ensureAdmin
import iz.mkao.mirasalon.server.util.getUserId
import iz.mkao.mirasalon.server.util.isAdmin
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

private val log = LoggerFactory.getLogger("SalonRoutes")

fun Route.salonRoutes(
    salonRepository: SalonRepository,
    appointmentRepository: AppointmentRepository,
    orderRepository: OrderRepository,
    specialistRepository: SpecialistRepository,
    serviceRepository: ServiceRepository,
    promotionRepository: PromotionRepository,
    appConfig: AppConfig
) {

    get("/home") {
        val userId = call.getUserId()

        val categories = serviceRepository.findNonEmptyCategories()
        val specialists = specialistRepository.findAll(null, 1, 10)

        val activePromotions = when (val result = promotionRepository.findActive(1, 10, userId)) {
            is Outcome.Success -> result.data.map { it.toDto() }
            else -> emptyList()
        }

        val homeData = SalonHomeDto(
            categories = categories,
            specialists = specialists,
            promotions = activePromotions,
            isLoggedIn = userId != null
        )

        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = homeData))
    }

    get("/image/{id}") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val path = salonRepository.getImagePath(id) ?: return@get call.respond(HttpStatusCode.NotFound)

        if (path.startsWith("http")) {
            return@get call.respondRedirect(path)
        }

        val cleanPath = path.removePrefix("/uploads/").removePrefix("/")
        val file = File(appConfig.uploadDir, cleanPath)
        if (file.exists()) {
            val contentType = ContentType.fromFilePath(file.name).firstOrNull() ?: ContentType.Image.Any
            call.respond(LocalFileContent(file, contentType))
        } else {
            log.warn("Salon image file not found: ${file.absolutePath} (from path: $path)")
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/details/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = "Salon ID required")
            )
            return@get
        }

        when (val result = salonRepository.findById(id)) {
            is SalonFetchResult.Success -> {
                log.debug("Fetched details for salon $id")
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.salon))
            }
            is SalonFetchResult.NotFound -> {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(success = false, error = "Salon not found")
                )
            }
        }
    }

    authenticate("auth-jwt") {

        get("/dashboard") {
            call.ensureAdmin()

            val timezone = ZoneId.systemDefault()
            val today = LocalDate.now(timezone)
            val todayStart = today.atStartOfDay(timezone).toInstant().toEpochMilli()
            val todayEnd = today.plusDays(1).atStartOfDay(timezone).toInstant().toEpochMilli()

            val confirmedToday = appointmentRepository.countByStatusInRange(AppointmentStatus.CONFIRMED, todayStart, todayEnd) +
                                 orderRepository.countByStatusInRange(OrderRepoStatus.PENDING, todayStart, todayEnd)
            val cancelledToday = appointmentRepository.countByStatusInRange(AppointmentStatus.CANCELLED, todayStart, todayEnd) +
                                 orderRepository.countByStatusInRange(OrderRepoStatus.CANCELLED, todayStart, todayEnd)

            val totalRevenueAllTime = appointmentRepository.totalRevenue() +
                                     orderRepository.totalRevenue()

            val totalAppointments = appointmentRepository.countAll()
            val dashboard = DashboardDto(
                totalSales = totalRevenueAllTime,
                totalAppointments = totalAppointments,
                totalRevenue = totalRevenueAllTime,
                confirmedAppointments = confirmedToday,
                cancelledAppointments = cancelledToday
            )

            log.info("Dashboard fetched by user ${call.getUserId()}")
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = dashboard))
        }

        get("/management") {
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

            val salons = salonRepository.findAll(page, pageSize)
            log.debug("Fetched ${salons.items.size} salons (page $page)")
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = salons))
        }

        put("/management/{id}") {
            if (!call.isAdmin()) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, error = "Admin access required")
                )
                return@put
            }

            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Salon ID required")
                )
                return@put
            }

            val salon = try {
                call.receive<UpdateSalonRequest>()
            } catch (e: Exception) {
                log.warn("Invalid salon update request: {}", e.message)
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Invalid request format")
                )
                return@put
            }

            val result = salonRepository.update(id, salon)
            when (result) {
                is SalonUpdateResult.Success -> {
                    log.info("Admin ${call.getUserId()} updated salon $id")
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                }
                is SalonUpdateResult.NotFound -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Salon not found"))
                }
                is SalonUpdateResult.Failure -> {
                    log.warn("Salon update failed: {}", result.message)
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }
    }
}
