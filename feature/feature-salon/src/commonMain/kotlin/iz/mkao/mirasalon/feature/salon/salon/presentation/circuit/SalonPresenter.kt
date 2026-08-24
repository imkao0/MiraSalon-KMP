package iz.mkao.mirasalon.feature.salon.salon.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.di.AppScope
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.core.domain.repository.PromoRepository
import iz.mkao.mirasalon.core.domain.repository.SalonRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceFavouritesRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceRepository
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.domain.repository.UnreadMessagesSource
import iz.mkao.mirasalon.core.domain.repository.UpcomingAppointmentsSource
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.NotificationRoute
import iz.mkao.mirasalon.core.navigation.ProfileRoute
import iz.mkao.mirasalon.core.navigation.ServiceRoute
import iz.mkao.mirasalon.core.navigation.SpecialistRoute
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.feature.profile.domain.repository.AddressRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.NotificationPreferencesRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.ProfileRepository
import iz.mkao.mirasalon.feature.salon.services.presentation.circuit.ServiceDetailPresenter
import iz.mkao.mirasalon.feature.salon.services.presentation.circuit.ServicesPresenter
import kotlinx.coroutines.launch

@CircuitInject(BottomNavKey.Home::class, AppScope::class)
class SalonPresenter(
    private val repository: SalonRepository,
    private val tokenProvider: SalonTokenProvider,
    private val profileRepository: ProfileRepository,
    private val navigator: Navigator,
    private val addressRepository: AddressRepository,
    private val promoRepository: PromoRepository? = null,
    private val notificationRepository: NotificationRepository? = null,
    private val notificationPreferencesRepository: NotificationPreferencesRepository? = null,
    private val unreadMessagesSource: UnreadMessagesSource? = null,
    private val upcomingAppointmentsSource: UpcomingAppointmentsSource? = null
) : Presenter<SalonState> {

    @Composable
    override fun present(): SalonState {
        var state by remember { mutableStateOf(SalonState()) }
        val scope = rememberCoroutineScope()

        val profileOutcome by profileRepository.observeProfile().collectAsState(initial = Outcome.Loading)


        val notifications by notificationRepository?.notifications?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
        val notificationPreferences by notificationPreferencesRepository?.observePreferences()?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
        val addresses by addressRepository.observeAddresses().collectAsState(initial = emptyList())
        val unreadMessagesCount by unreadMessagesSource?.observeUnreadMessagesCount()?.collectAsState(initial = 0) ?: remember { mutableStateOf(0) }
        val upcomingAppointmentsCount by upcomingAppointmentsSource?.observeUpcomingAppointmentsCount()?.collectAsState(initial = 0) ?: remember { mutableStateOf(0) }

        fun loadHome() {
            scope.launch {
                state = state.copy(isLoading = true, errorMessage = null)
                

                val name = tokenProvider.userName()

                val defaultAddress = addresses.find { it.isDefault }?.streetAddress
                val address = defaultAddress ?: tokenProvider.userAddress()
                val avatarUrl = tokenProvider.userAvatarUrl()
                
                when (val result = repository.getHome()) {
                    is Outcome.Success -> {
                        state = state.copy(
                            isLoading = false,
                            userName = name,
                            userLocation = address,
                            userAvatarUrl = avatarUrl,
                            categories = result.data.categories,
                            specialists = result.data.specialists,
                            promotions = result.data.promotions,
                            isLoggedIn = result.data.isLoggedIn
                        )
                    }
                    is Outcome.Error -> {
                        Napier.w("Salon home failed to load: ${result.failure}")
                        state = state.copy(
                            isLoading = false,
                            errorMessage = result.failure.toErrorMessage()
                        )
                    }
                    is Outcome.Loading -> Unit
                }
            }
        }

        LaunchedEffect(Unit) {
            loadHome()
            addressRepository.refresh()

            notificationRepository?.fetchNotifications()

            // Fetch used promotion IDs
            promoRepository?.let { repo ->
                when (val result = repo.getUsedPromotionIds()) {
                    is Outcome.Success -> {
                        state = state.copy(usedPromotionIds = result.data.toSet())
                    }
                    is Outcome.Error -> {
                        Napier.w("Failed to fetch used promotion IDs: ${result.failure}")
                    }
                    is Outcome.Loading -> Unit
                }
            }
        }


        LaunchedEffect(profileOutcome) {
            if (profileOutcome is Outcome.Success) {
                val profile = (profileOutcome as Outcome.Success).data
                state = state.copy(
                    userName = profile.fullName,
                    userAvatarUrl = profile.avatarUrl
                )
            }
        }


        LaunchedEffect(addresses) {
            val defaultAddress = addresses.find { it.isDefault }?.streetAddress
            if (defaultAddress != null) {
                state = state.copy(userLocation = defaultAddress)
            }
        }


        LaunchedEffect(notifications, notificationPreferences, unreadMessagesCount, upcomingAppointmentsCount) {
            val unreadCount = notifications.count { it.isUnread } + unreadMessagesCount + upcomingAppointmentsCount
            val inAppEnabled = notificationPreferences?.inAppEnabled ?: true
            state = state.copy(
                unreadNotificationCount = unreadCount,
                inAppNotificationsEnabled = inAppEnabled
            )
        }

        val finalState = state.copy(
            eventSink = { event ->
                Napier.d("SalonPresenter: Event received: $event")
                when (event) {
                    SalonEvent.NotificationClicked -> {
                        navigator.goTo(NotificationRoute.Notifications)
                    }
                    SalonEvent.FavoriteClicked -> {
                        navigator.goTo(ProfileRoute.Favourites)
                    }
                    is SalonEvent.CategorySelected -> {
                        state = state.copy(selectedCategoryId = event.id)
                        navigator.goTo(ServiceRoute.Services(categoryId = event.id))
                    }
                    SalonEvent.ViewAllCategories -> {
                        navigator.goTo(ServiceRoute.Services(categoryId = null))
                    }
                    is SalonEvent.SpecialistSelected -> {
                        navigator.goTo(SpecialistRoute.SpecialistDetail(specialistId = event.id))
                    }
                    SalonEvent.ViewAllSpecialists -> {
                        navigator.goTo(SpecialistRoute.Specialists)
                    }
                    is SalonEvent.PromotionClicked -> {
                        val code = state.promotions.find { it.id == event.id }?.code
                        if (!code.isNullOrBlank()) {
                            state = state.copy(promotionCodeToCopy = code)
                        }
                    }
                    is SalonEvent.SearchQueryChanged -> {
                        state = state.copy(searchQuery = event.query)
                    }
                    SalonEvent.PromotionCodeConsumed -> {
                        state = state.copy(promotionCodeToCopy = null)
                    }
                    SalonEvent.Retry -> loadHome()
                }
            }
        )
        Napier.d("SalonPresenter: Emitting state: avatar=${finalState.userAvatarUrl}")
        return finalState
    }
}

class SalonManualPresenterFactory(
    private val salonRepository: SalonRepository,
    private val tokenProvider: SalonTokenProvider,
    private val profileRepository: ProfileRepository,
    private val addressRepository: AddressRepository,
    private val serviceRepository: ServiceRepository,
    private val serviceFavouritesRepository: ServiceFavouritesRepository,
    private val specialistRepository: SpecialistRepository,
    private val promoRepository: PromoRepository? = null,
    private val notificationRepository: NotificationRepository? = null,
    private val notificationPreferencesRepository: NotificationPreferencesRepository? = null,
    private val unreadMessagesSource: UnreadMessagesSource? = null,
    private val upcomingAppointmentsSource: UpcomingAppointmentsSource? = null
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is BottomNavKey.Home -> SalonPresenter(
                repository = salonRepository,
                tokenProvider = tokenProvider,
                profileRepository = profileRepository,
                navigator = navigator,
                addressRepository = addressRepository,
                promoRepository = promoRepository,
                notificationRepository = notificationRepository,
                notificationPreferencesRepository = notificationPreferencesRepository,
                unreadMessagesSource = unreadMessagesSource,
                upcomingAppointmentsSource = upcomingAppointmentsSource
            )
            is ServiceRoute.Services -> ServicesPresenter(
                screen,
                serviceRepository,
                salonRepository,
                promoRepository ?: throw IllegalStateException("PromoRepository not provided"),
                navigator
            )
            is ServiceRoute.ServiceDetail -> ServiceDetailPresenter(
                screen,
                serviceRepository,
                serviceFavouritesRepository,
                specialistRepository,
                notificationRepository ?: throw IllegalStateException("NotificationRepository not provided"),
                navigator
            )
            else -> null
        }
    }
}

private fun Failure.toErrorMessage(): String = when (this) {
    is Failure.NetworkConnection -> message
    is Failure.ServerError -> message
    is Failure.ClientError -> message
    is Failure.SessionExpired -> "Session expired"
    is Failure.Unknown -> "Couldn't load the salon home page"
}
