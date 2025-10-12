package iz.mkao.mirasalon.presentation

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
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
import iz.mkao.mirasalon.core.domain.repository.ProductRepository
import iz.mkao.mirasalon.core.domain.repository.ReviewsRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceRepository
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.domain.repository.StreamChatManager
import iz.mkao.mirasalon.core.domain.repository.UploadRepository
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.data.local.TokenManager
import iz.mkao.mirasalon.presentation.analytics.AnalyticsPresenter
import iz.mkao.mirasalon.presentation.bookings.BookingsPresenter
import iz.mkao.mirasalon.presentation.calendar.CalendarPresenter
import iz.mkao.mirasalon.presentation.chat.AdminChatPresenter
import iz.mkao.mirasalon.presentation.customers.CustomersPresenter
import iz.mkao.mirasalon.presentation.dashboard.DashboardPresenter
import iz.mkao.mirasalon.presentation.help.HelpPresenter
import iz.mkao.mirasalon.presentation.orders.OrdersPresenter
import iz.mkao.mirasalon.presentation.products.ProductsPresenter
import iz.mkao.mirasalon.presentation.promotions.PromotionsPresenter
import iz.mkao.mirasalon.presentation.reviews.ReviewsPresenter
import iz.mkao.mirasalon.presentation.services.ServicesPresenter
import iz.mkao.mirasalon.presentation.settings.SettingsPresenter
import iz.mkao.mirasalon.presentation.staff.StaffPresenter

/** Circuit [Presenter.Factory] creating the desktop admin presenters. */
class DesktopPresenterFactory(
    private val dashboardRepository: DashboardRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val adminServiceRepository: AdminServiceRepository,
    private val serviceRepository: ServiceRepository,
    private val adminPromotionRepository: AdminPromotionRepository,
    private val adminSpecialistRepository: AdminSpecialistRepository,
    private val specialistRepository: SpecialistRepository,
    private val bookingsRepository: BookingsRepository,
    private val adminOrderRepository: AdminOrderRepository,
    private val reviewsRepository: ReviewsRepository,
    private val adminSalonRepository: AdminSalonRepository,
    private val streamChatManager: StreamChatManager,
    private val notificationRepository: NotificationRepository,
    private val uploadRepository: UploadRepository,
    private val tokenManager: TokenManager,
    private val realtimeGateway: RealtimeGateway,
    private val dispatcherProvider: DispatcherProvider
) : Presenter.Factory {

    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? =
        when (screen) {
            is DesktopScreen.Dashboard -> DashboardPresenter(
                dashboardRepository,
                tokenManager,
                realtimeGateway
            )
            is DesktopScreen.Analytics -> AnalyticsPresenter(
                dashboardRepository,
                realtimeGateway
            )
            is DesktopScreen.Products -> ProductsPresenter(
                productRepository,
                uploadRepository,
                realtimeGateway,
                dispatcherProvider.io
            )
            is DesktopScreen.Customers -> CustomersPresenter(
                customerRepository,
                dashboardRepository,
                reviewsRepository,
                adminOrderRepository
            )
            is DesktopScreen.Services -> ServicesPresenter(adminServiceRepository, uploadRepository)
            is DesktopScreen.Promotions -> PromotionsPresenter(
                adminPromotionRepository,
                uploadRepository,
                adminServiceRepository,
                productRepository
            )
            is DesktopScreen.Staff -> StaffPresenter(
                adminSpecialistRepository,
                uploadRepository,
                serviceRepository,
                realtimeGateway
            )
            is DesktopScreen.Bookings -> BookingsPresenter(
                bookingsRepository,
                specialistRepository,
                tokenManager,
                realtimeGateway
            )
            is DesktopScreen.Calendar -> CalendarPresenter(
                specialistRepository,
                bookingsRepository,
                customerRepository,
                serviceRepository,
                adminSalonRepository,
                tokenManager
            )
            is DesktopScreen.Orders -> OrdersPresenter(adminOrderRepository)
            is DesktopScreen.Reviews -> ReviewsPresenter(reviewsRepository)
            is DesktopScreen.Settings -> SettingsPresenter(adminSalonRepository)
            is DesktopScreen.Chat -> AdminChatPresenter(
                streamChatManager,
                specialistRepository,
                notificationRepository,
                tokenManager,
                uploadRepository,
                realtimeGateway,
                dispatcherProvider.io,
                screen.sessionId
            )
            is DesktopScreen.Help -> HelpPresenter()
            else -> null
        }
}
