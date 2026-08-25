package iz.mkao.mirasalon.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.slack.circuit.foundation.Circuit
import iz.mkao.mirasalon.core.common.util.DefaultDispatcherProvider
import iz.mkao.mirasalon.core.common.util.DispatcherProvider
import iz.mkao.mirasalon.core.domain.repository.AdminOrderRepository
import iz.mkao.mirasalon.core.domain.repository.AdminPromotionRepository
import iz.mkao.mirasalon.core.domain.repository.AdminSalonRepository
import iz.mkao.mirasalon.core.domain.repository.AdminServiceRepository
import iz.mkao.mirasalon.core.domain.repository.AdminSpecialistRepository
import iz.mkao.mirasalon.core.domain.repository.BookingsRepository
import iz.mkao.mirasalon.core.domain.repository.CustomerRepository
import iz.mkao.mirasalon.core.domain.repository.DashboardRepository
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.core.domain.repository.ReviewsRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceRepository
import iz.mkao.mirasalon.core.domain.repository.UploadRepository
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.client.admin.AdminBookingsApi
import iz.mkao.mirasalon.core.network.client.admin.AdminCustomerApi
import iz.mkao.mirasalon.core.network.client.admin.AdminOrdersApi
import iz.mkao.mirasalon.core.network.client.admin.AdminPromotionApi
import iz.mkao.mirasalon.core.network.client.admin.AdminReviewApi
import iz.mkao.mirasalon.core.network.client.admin.AdminSalonApi
import iz.mkao.mirasalon.core.network.client.admin.AdminServicesApi
import iz.mkao.mirasalon.core.network.client.admin.AdminStaffApi
import iz.mkao.mirasalon.core.network.client.admin.DashboardApi
import iz.mkao.mirasalon.core.network.client.admin.KtorAdminBookingsApi
import iz.mkao.mirasalon.core.network.client.admin.KtorAdminCustomerApi
import iz.mkao.mirasalon.core.network.client.admin.KtorAdminOrdersApi
import iz.mkao.mirasalon.core.network.client.admin.KtorAdminPromotionApi
import iz.mkao.mirasalon.core.network.client.admin.KtorAdminReviewApi
import iz.mkao.mirasalon.core.network.client.admin.KtorAdminSalonApi
import iz.mkao.mirasalon.core.network.client.admin.KtorAdminServicesApi
import iz.mkao.mirasalon.core.network.client.admin.KtorAdminStaffApi
import iz.mkao.mirasalon.core.network.client.admin.KtorDashboardApi
import iz.mkao.mirasalon.core.network.client.provideBaseUrl
import iz.mkao.mirasalon.core.network.client.provideWebSocketUrl
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.SalonApiConfig
import iz.mkao.mirasalon.core.network.repository.DefaultUploadRepository
import iz.mkao.mirasalon.data.local.TokenManager
import iz.mkao.mirasalon.data.remote.AuthClient
import iz.mkao.mirasalon.data.remote.DesktopNotificationService
import iz.mkao.mirasalon.data.repository.DesktopNotificationRepository
import iz.mkao.mirasalon.data.repository.KtorAdminOrderRepository
import iz.mkao.mirasalon.data.repository.KtorAdminPromotionRepository
import iz.mkao.mirasalon.data.repository.KtorAdminReviewRepository
import iz.mkao.mirasalon.data.repository.KtorAdminSalonRepository
import iz.mkao.mirasalon.data.repository.KtorAdminServiceRepository
import iz.mkao.mirasalon.data.repository.KtorAdminSpecialistRepository
import iz.mkao.mirasalon.data.repository.KtorBookingsRepository
import iz.mkao.mirasalon.data.repository.KtorCustomerRepository
import iz.mkao.mirasalon.data.repository.KtorDashboardRepository
import iz.mkao.mirasalon.data.repository.KtorServiceRepository
import iz.mkao.mirasalon.feature.auth.presentation.circuit.AuthManualPresenterFactory
import iz.mkao.mirasalon.feature.auth.presentation.circuit.AuthManualUiFactory
import iz.mkao.mirasalon.feature.notifications.data.repository.createDesktopNotifier
import iz.mkao.mirasalon.presentation.DesktopPresenterFactory
import iz.mkao.mirasalon.presentation.analytics.AnalyticsUiFactory
import iz.mkao.mirasalon.presentation.bookings.BookingsUiFactory
import iz.mkao.mirasalon.presentation.calendar.CalendarUiFactory
import iz.mkao.mirasalon.presentation.chat.AdminChatUiFactory
import iz.mkao.mirasalon.presentation.customers.CustomersUiFactory
import iz.mkao.mirasalon.presentation.dashboard.DesktopDashboardUiFactory
import iz.mkao.mirasalon.presentation.help.HelpUiFactory
import iz.mkao.mirasalon.presentation.orders.OrdersUiFactory
import iz.mkao.mirasalon.presentation.products.ProductsUiFactory
import iz.mkao.mirasalon.presentation.promotions.PromotionsUiFactory
import iz.mkao.mirasalon.presentation.reviews.ReviewsUiFactory
import iz.mkao.mirasalon.presentation.services.ServicesUiFactory
import iz.mkao.mirasalon.presentation.settings.SettingsUiFactory
import iz.mkao.mirasalon.presentation.staff.StaffUiFactory
import iz.mkao.mirasalon.util.DesktopConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.util.prefs.Preferences

val desktopModule = module {

    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single<Settings> {
        PreferencesSettings(Preferences.userRoot().node("iz.mkao.mirasalon"))
    }

    singleOf(::TokenManager)
    single<SalonTokenProvider> { get<TokenManager>() }
    singleOf(::AuthClient)
    singleOf(::DesktopNotificationService)

    single<SalonApiConfig> {
        val baseUrl = provideBaseUrl()
        val wsUrl = provideWebSocketUrl()
        ApiEndpoints.setBaseUrl(baseUrl)
        SalonApiConfig(
            baseUrl = baseUrl,
            webSocketUrl = wsUrl,
            streamApiKey = DesktopConfig.streamApiKey,
            streamApiSecret = DesktopConfig.streamApiSecret,
            enableLogging = true
        )
    }

    single<AdminStaffApi> { KtorAdminStaffApi(get()) }
    single<AdminCustomerApi> { KtorAdminCustomerApi(get()) }
    single<AdminReviewApi> { KtorAdminReviewApi(get()) }
    single<AdminPromotionApi> { KtorAdminPromotionApi(get()) }
    single<DashboardApi> { KtorDashboardApi(get()) }
    single<AdminServicesApi> { KtorAdminServicesApi(get()) }
    single<AdminBookingsApi> { KtorAdminBookingsApi(get()) }
    single<AdminOrdersApi> { KtorAdminOrdersApi(get()) }
    single<AdminSalonApi> { KtorAdminSalonApi(get()) }


    single<UploadRepository> { DefaultUploadRepository(get()) }
    single<CustomerRepository> { KtorCustomerRepository(get()) }
    single<BookingsRepository> { KtorBookingsRepository(get()) }
    single<AdminSpecialistRepository> { KtorAdminSpecialistRepository(get()) }
    single<AdminPromotionRepository> { KtorAdminPromotionRepository(get()) }
    single<AdminOrderRepository> { KtorAdminOrderRepository(get()) }
    single<ReviewsRepository> { KtorAdminReviewRepository(get()) }
    single<DashboardRepository> { KtorDashboardRepository(get()) }
    single<AdminSalonRepository> { KtorAdminSalonRepository(get()) }
    single<AdminServiceRepository> { KtorAdminServiceRepository(get()) }
    single<ServiceRepository> { KtorServiceRepository(get()) }
    single<NotificationRepository> { DesktopNotificationRepository(createDesktopNotifier()) }


    single {
        DesktopPresenterFactory(
            dashboardRepository = get(),
            productRepository = get(),
            customerRepository = get(),
            adminServiceRepository = get(),
            serviceRepository = get(),
            adminPromotionRepository = get(),
            adminSpecialistRepository = get(),
            specialistRepository = get(),
            bookingsRepository = get(),
            adminOrderRepository = get(),
            reviewsRepository = get(),
            adminSalonRepository = get(),
            streamChatManager = get(),
            notificationRepository = get(),
            uploadRepository = get(),
            tokenManager = get(),
            realtimeGateway = get(),
            dispatcherProvider = get()
        )
    }


    single {
        Circuit.Builder()
            .addPresenterFactory(get<DesktopPresenterFactory>())
            .addPresenterFactory(get<AuthManualPresenterFactory>())
            .addUiFactory(AuthManualUiFactory())
            .addUiFactories(
                listOf(
                    DesktopDashboardUiFactory(),
                    AnalyticsUiFactory(),
                    ProductsUiFactory(),
                    CustomersUiFactory(),
                    ServicesUiFactory(),
                    PromotionsUiFactory(),
                    StaffUiFactory(),
                    BookingsUiFactory(),
                    CalendarUiFactory(),
                    OrdersUiFactory(),
                    ReviewsUiFactory(),
                    SettingsUiFactory(),
                    HelpUiFactory(),
                    AdminChatUiFactory()
                )
            )
            .build()
    }
}
