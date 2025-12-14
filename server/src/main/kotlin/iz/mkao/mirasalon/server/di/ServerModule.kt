package iz.mkao.mirasalon.server.di

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import iz.mkao.mirasalon.server.data.repository.AppointmentRepository
import iz.mkao.mirasalon.server.data.repository.CustomerRepository
import iz.mkao.mirasalon.server.data.repository.OrderRepository
import iz.mkao.mirasalon.server.data.repository.OutboxRepository
import iz.mkao.mirasalon.server.data.repository.ProductRepository
import iz.mkao.mirasalon.server.data.repository.PromotionRepository
import iz.mkao.mirasalon.server.data.repository.RefreshTokenRepository
import iz.mkao.mirasalon.server.data.repository.ReviewRepository
import iz.mkao.mirasalon.server.data.repository.SalonRepository
import iz.mkao.mirasalon.server.data.repository.ServiceRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistAvailabilityRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistClientNotesRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.service.NotificationService
import iz.mkao.mirasalon.server.service.OutboxDispatcher
import iz.mkao.mirasalon.server.service.StreamSyncService
import iz.mkao.mirasalon.server.storage.LocalStorageService
import iz.mkao.mirasalon.server.storage.StorageService
import iz.mkao.mirasalon.server.util.AppConfig
import iz.mkao.mirasalon.server.util.JwtConfig
import iz.mkao.mirasalon.server.util.ServerEnvironment
import iz.mkao.mirasalon.server.util.StartupTasks
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File
import java.time.Clock
import java.time.ZoneId

/**
 * Koin dependency injection module.
 * All services, repositories, and configuration are defined here
 */
val serverModule = module {

    single<CoroutineDispatcher> { Dispatchers.IO }
    single<Clock> { Clock.systemUTC() }
    single<ZoneId> { ZoneId.systemDefault() }
    single {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            coerceInputValues = true
        }
    }
    singleOf(::OutboxRepository)
    single { iz.mkao.mirasalon.server.realtime.RealtimeSessionRegistry() }
    single {
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT).apply {
            config().commonTags(
                "application", "mirasalon",
                "environment", ServerEnvironment.environment()
            )
        }
    }
    single<MeterRegistry> { get<PrometheusMeterRegistry>() }
    
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single {
        OutboxDispatcher(get(), get(), get())
    }

    single {
        AppConfig(
            streamApiKey = ServerEnvironment.secretOrNull("STREAM_API_KEY").orEmpty(),
            streamApiSecret = ServerEnvironment.secretOrNull("STREAM_API_SECRET").orEmpty(),
            streamAppId = ServerEnvironment.secretOrNull("STREAM_APP_ID").orEmpty(),

            uploadDir = ServerEnvironment.orDefault("UPLOAD_DIR", run {
                val rootUploads = File("server/uploads")
                val nestedUploads = File("server/server/uploads")
                when {
                    nestedUploads.exists() -> "server/server/uploads"
                    rootUploads.exists() -> "server/uploads"
                    else -> "./uploads"
                }
            }),

            environment = ServerEnvironment.environment()
        )
    }

    single {
        JwtConfig(
            secret = ServerEnvironment.secret("JWT_SECRET"),
            issuer = ServerEnvironment.orDefault("JWT_ISSUER", "mirasalon"),
            audience = ServerEnvironment.orDefault("JWT_AUDIENCE", "mirasalon"),
            realm = ServerEnvironment.orDefault("JWT_REALM", "mirasalon"),
            expiration = ServerEnvironment.optional("JWT_EXPIRATION")?.toLong()
                ?: 3_600_000L // 1 hour
        )
    }

    singleOf(::UserRepository)
    singleOf(::RefreshTokenRepository)
    singleOf(::SalonRepository)
    singleOf(::AppointmentRepository)
    singleOf(::ProductRepository)
    singleOf(::PromotionRepository)
    singleOf(::ServiceRepository)
    single { 
        SpecialistRepository(get(), get(), get(), get(), get())
    }
    singleOf(::SpecialistAvailabilityRepository)
    singleOf(::SpecialistClientNotesRepository)
    single { CustomerRepository(get()) }
    singleOf(::ReviewRepository)
    singleOf(::OrderRepository)

    singleOf(::NotificationService)

    single {
        val config = get<AppConfig>()
        StreamSyncService(
            apiKey = config.streamApiKey,
            apiSecret = config.streamApiSecret
        )
    }

    single<StorageService> {
        val config = get<AppConfig>()
        LocalStorageService(uploadDir = config.uploadDir)
    }

    single {
        StartupTasks(
            appConfig = get(),
            streamSyncService = get(),
            orderRepository = get()
        )
    }
}
