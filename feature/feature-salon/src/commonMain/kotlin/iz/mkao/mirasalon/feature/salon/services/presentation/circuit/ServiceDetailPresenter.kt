package iz.mkao.mirasalon.feature.salon.services.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.common.di.AppScope
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceFilter
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceFavouritesRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceRepository
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.navigation.BookingRoute
import iz.mkao.mirasalon.core.navigation.NotificationRoute
import iz.mkao.mirasalon.core.navigation.ServiceRoute
import iz.mkao.mirasalon.core.navigation.SpecialistRoute
import kotlinx.coroutines.launch

@CircuitInject(ServiceRoute.ServiceDetail::class, AppScope::class)
class ServiceDetailPresenter(
    private val screen: ServiceRoute.ServiceDetail,
    private val repository: ServiceRepository,
    private val serviceFavouritesRepository: ServiceFavouritesRepository,
    private val specialistRepository: SpecialistRepository,
    private val notificationRepository: NotificationRepository,
    private val navigator: Navigator
) : Presenter<ServiceDetailState> {

    @Composable
    override fun present(): ServiceDetailState {
        var isLoading by remember { mutableStateOf(false) }
        var service by remember { mutableStateOf<Service?>(null) }
        var categoryName by remember { mutableStateOf<String?>(null) }
        var categoryIconKey by remember { mutableStateOf<String?>(null) }
        var isFavorited by remember { mutableStateOf(false) }
        var isBookmarked by remember { mutableStateOf(false) }
        var specialists by remember { mutableStateOf<List<Specialist>>(emptyList()) }
        var relatedServices by remember { mutableStateOf<List<Service>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        val unreadNotificationCount by notificationRepository.unreadCount.collectAsState(initial = 0)

        fun loadService() {
            scope.launch {
                isLoading = true
                when (val result = repository.getService(screen.serviceId)) {
                    is Outcome.Success -> {
                        val s = result.data
                        service = s
                        isFavorited = serviceFavouritesRepository.isFavourite(screen.serviceId)

                        // Fetch category name and icon
                        when (val categoriesResult = repository.getCategories()) {
                            is Outcome.Success -> {
                                val category = categoriesResult.data.find { it.id == s.categoryId }
                                categoryName = category?.name
                                categoryIconKey = category?.iconName ?: category?.name
                            }
                            else -> {
                                // Fallback to categoryId if failed to fetch categories
                                categoryName = s.categoryId
                                categoryIconKey = s.categoryId
                            }
                        }

                        // Fetch specialists who provide this service
                        when (val specialistsResult = specialistRepository.getSpecialists()) {
                            is Outcome.Success -> {
                                specialists = specialistsResult.data.filter { specialist ->
                                    specialist.services.any { it.id == s.id }
                                }
                            }
                            else -> {
                                specialists = emptyList()
                            }
                        }

                        // Fetch related services (same category)
                        when (val relatedResult = repository.getServices(ServiceFilter(categoryId = s.categoryId))) {
                            is Outcome.Success -> {
                                relatedServices = relatedResult.data.filter { it.id != s.id }.take(4)
                            }
                            else -> {
                                relatedServices = emptyList()
                            }
                        }

                        isLoading = false
                    }
                    is Outcome.Error -> {
                        error = "Failed to load service detail"
                        isLoading = false
                    }
                    else -> isLoading = false
                }
            }
        }

        LaunchedEffect(Unit) {
            loadService()
        }

        return ServiceDetailState(
            isLoading = isLoading,
            service = service,
            categoryName = categoryName,
            categoryIconKey = categoryIconKey,
            isFavorited = isFavorited,
            isBookmarked = isBookmarked,
            specialists = specialists,
            relatedServices = relatedServices,
            error = error,
            unreadNotificationCount = unreadNotificationCount,
            onReviewSubmit = { rating, comment ->
                val result = repository.submitReview(screen.serviceId, rating, comment)
                when (result) {
                    is Outcome.Success -> {
                        loadService()
                        Result.success(Unit)
                    }
                    is Outcome.Error -> {
                        val message = when (val failure = result.failure) {
                            is Failure.ServerError -> failure.message
                            is Failure.ClientError -> failure.message
                            is Failure.NetworkConnection -> failure.message
                            else -> failure.toString()
                        }
                        Result.failure(Exception(message))
                    }
                    else -> Result.failure(Exception("Unknown error"))
                }
            },
            eventSink = { event ->
                when (event) {
                    ServiceDetailEvent.BackClicked -> navigator.pop()
                    ServiceDetailEvent.Retry -> loadService()
                    ServiceDetailEvent.BookClicked -> {
                        navigator.goTo(BookingRoute.Booking(serviceIds = listOf(screen.serviceId)))
                    }
                    ServiceDetailEvent.ToggleFavorite -> {
                        service?.let { s ->
                            scope.launch {
                                if (isFavorited) {
                                    serviceFavouritesRepository.removeServiceFromFavourites(s.id)
                                } else {
                                    serviceFavouritesRepository.addServiceToFavourites(s)
                                }
                                isFavorited = !isFavorited
                            }
                        }
                    }
                    ServiceDetailEvent.ToggleBookmark -> {
                        isBookmarked = !isBookmarked
                    }
                    ServiceDetailEvent.SaveClicked -> {
                        service?.let { s ->
                            scope.launch {
                                if (!isFavorited) {
                                    serviceFavouritesRepository.addServiceToFavourites(s)
                                    isFavorited = true
                                }
                            }
                        }
                    }
                    ServiceDetailEvent.NotificationClicked -> {
                        navigator.goTo(NotificationRoute.Notifications)
                    }
                    is ServiceDetailEvent.SpecialistClicked -> {
                        navigator.goTo(SpecialistRoute.SpecialistDetail(event.specialistId))
                    }
                    is ServiceDetailEvent.RelatedServiceClicked -> {
                        navigator.goTo(ServiceRoute.ServiceDetail(event.serviceId))
                    }
                }
            }
        )
    }
}
