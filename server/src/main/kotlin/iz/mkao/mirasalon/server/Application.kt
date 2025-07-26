package iz.mkao.mirasalon.server

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.MeterRegistry
import iz.mkao.mirasalon.server.data.repository.AppointmentRepository
import iz.mkao.mirasalon.server.data.repository.CustomerRepository
import iz.mkao.mirasalon.server.data.repository.OrderRepository
import iz.mkao.mirasalon.server.data.repository.ProductRepository
import iz.mkao.mirasalon.server.data.repository.PromotionRepository
import iz.mkao.mirasalon.server.data.repository.RefreshTokenRepository
import iz.mkao.mirasalon.server.data.repository.ReviewRepository
import iz.mkao.mirasalon.server.data.repository.SalonRepository
import iz.mkao.mirasalon.server.data.repository.ServiceRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistAvailabilityRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.di.serverModule
import iz.mkao.mirasalon.server.domain.admin.adminStaffRoutes
import iz.mkao.mirasalon.server.domain.analytics.analyticsRoutes
import iz.mkao.mirasalon.server.domain.auth.authRoutes
import iz.mkao.mirasalon.server.domain.booking.bookingRoutes
import iz.mkao.mirasalon.server.domain.customer.customerRoutes
import iz.mkao.mirasalon.server.domain.customer.profileRoutes
import iz.mkao.mirasalon.server.domain.notification.notificationRoutes
import iz.mkao.mirasalon.server.domain.order.orderRoutes
import iz.mkao.mirasalon.server.domain.products.productRoutes
import iz.mkao.mirasalon.server.domain.promotion.promotionRoutes
import iz.mkao.mirasalon.server.domain.review.reviewRoutes
import iz.mkao.mirasalon.server.domain.salon.salonRoutes
import iz.mkao.mirasalon.server.domain.salon.serviceRoutes
import iz.mkao.mirasalon.server.domain.salon.specialistRoutes
import iz.mkao.mirasalon.server.domain.stream.streamRoutes
import iz.mkao.mirasalon.server.domain.upload.uploadRoutes
import iz.mkao.mirasalon.server.plugins.configureAuthentication
import iz.mkao.mirasalon.server.plugins.configureCORS
import iz.mkao.mirasalon.server.plugins.configureCallId
import iz.mkao.mirasalon.server.plugins.configureLogging
import iz.mkao.mirasalon.server.plugins.configureMetrics
import iz.mkao.mirasalon.server.plugins.configureRateLimiting
import iz.mkao.mirasalon.server.plugins.configureSecureHeaders
import iz.mkao.mirasalon.server.plugins.configureSerialization
import iz.mkao.mirasalon.server.plugins.configureStatusPages
import iz.mkao.mirasalon.server.plugins.configureWebSockets
import iz.mkao.mirasalon.server.realtime.RealtimeSessionRegistry
import iz.mkao.mirasalon.server.service.NotificationService
import iz.mkao.mirasalon.server.service.OutboxDispatcher
import iz.mkao.mirasalon.server.service.StreamSyncService
import iz.mkao.mirasalon.server.storage.StorageService
import iz.mkao.mirasalon.server.util.AppConfig
import iz.mkao.mirasalon.server.util.DatabaseFactory
import iz.mkao.mirasalon.server.util.DebugUtils
import iz.mkao.mirasalon.server.util.JwtConfig
import iz.mkao.mirasalon.server.util.StartupTasks
import iz.mkao.mirasalon.server.websocket.chatWebSocket
import iz.mkao.mirasalon.server.websocket.notificationWebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import java.io.File

fun main(args: Array<String>) {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(Koin) {
        modules(serverModule)
    }

    val config by inject<AppConfig>()
    val jwtConfig by inject<JwtConfig>()

    DatabaseFactory.init(
        url = System.getenv("DATABASE_URL") ?: "jdbc:h2:mem:mirasalon;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        driver = System.getenv("DATABASE_DRIVER") ?: "org.h2.Driver",
        user = System.getenv("DATABASE_USER") ?: "sa",
        password = System.getenv("DATABASE_PASSWORD") ?: ""
    )
    DebugUtils.logUsers()

    configureSerialization()
    install(AutoHeadResponse)
    configureAuthentication(jwtConfig)
    configureCallId()
    configureLogging()
    configureStatusPages()
    configureWebSockets()
    configureCORS()
    configureMetrics()
    configureRateLimiting()
    configureSecureHeaders()

    val userRepository by inject<UserRepository>()
    val refreshTokenRepository by inject<RefreshTokenRepository>()
    val salonRepository by inject<SalonRepository>()
    val appointmentRepository by inject<AppointmentRepository>()
    val productRepository by inject<ProductRepository>()
    val promotionRepository by inject<PromotionRepository>()
    val serviceRepository by inject<ServiceRepository>()
    val specialistRepository by inject<SpecialistRepository>()
    
    // Register metrics after DB init
    productRepository.registerMetrics()
    specialistRepository.registerMetrics()
    val specialistAvailabilityRepository by inject<SpecialistAvailabilityRepository>()
    val customerRepository by inject<CustomerRepository>()
    val reviewRepository by inject<ReviewRepository>()
    val orderRepository by inject<OrderRepository>()
    val storageService by inject<StorageService>()

    val streamSyncService by inject<StreamSyncService>()
    val notificationService by inject<NotificationService>()
    val outboxDispatcher by inject<OutboxDispatcher>()
    val realtimeRegistry by inject<RealtimeSessionRegistry>()
    val meterRegistry by inject<MeterRegistry>()
    val appScope by inject<CoroutineScope>()
    val startupTasks by inject<StartupTasks>()

    routing {
        staticFiles("/uploads", File(config.uploadDir))

        route("/v1/api") {
            route("/auth") { authRoutes(userRepository, jwtConfig, streamSyncService, refreshTokenRepository) }
            route("/salon") { salonRoutes(salonRepository, appointmentRepository, orderRepository, specialistRepository, serviceRepository, promotionRepository, config) }
            route("/services") { serviceRoutes(serviceRepository, config) }
            route("/bookings") { bookingRoutes(appointmentRepository) }
            route("/specialists") {
                specialistRoutes(specialistRepository, specialistAvailabilityRepository, config)
            }

            route("/admin") {
                route("/staff") {
                    adminStaffRoutes(specialistRepository)
                }
            }

            route("/analytics") { analyticsRoutes(appointmentRepository, orderRepository, specialistRepository, serviceRepository, meterRegistry) }
            route("/customers") { customerRoutes(customerRepository, userRepository, config) }
            route("/notifications") { notificationRoutes() }
            route("/profile") { profileRoutes(userRepository, storageService, config) }
            route("/orders") { orderRoutes(orderRepository) }
            route("/products") { productRoutes(productRepository, config) }
            route("/promotions") { promotionRoutes(promotionRepository, config) }
            route("/reviews") { reviewRoutes(reviewRepository) }
            route("/stream") {
                streamRoutes(
                    streamApiKey = config.streamApiKey,
                    streamApiSecret = config.streamApiSecret,
                    streamAppId = config.streamAppId,
                    userRepository = userRepository,
                    specialistRepository = specialistRepository,
                    streamSyncService = streamSyncService,
                    notificationService = notificationService
                )
            }
            route("/upload") {
                uploadRoutes(storageService)
            }
        }

        notificationWebSocket(realtimeRegistry)
        chatWebSocket(
            registry = realtimeRegistry,
            streamSyncService = streamSyncService,
            userRepository = userRepository,
            specialistRepository = specialistRepository,
            notificationService = notificationService
        )
    }

    notificationService.startReminderTask()
    outboxDispatcher.start()
    appointmentRepository.startAutoCompletionScheduler()

    startupTasks.run(appScope)

    monitor.subscribe(ApplicationStopped) {
        notificationService.stop()
        outboxDispatcher.stop()
        appointmentRepository.stopAutoCompletionScheduler()
        appScope.cancel()
    }
}
