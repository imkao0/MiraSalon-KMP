package iz.mkao.mirasalon.feature.profile.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.PaymentMethodRepository
import iz.mkao.mirasalon.core.navigation.AuthRoute
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.CartRoute
import iz.mkao.mirasalon.core.navigation.NotificationRoute
import iz.mkao.mirasalon.core.navigation.ProfileRoute
import iz.mkao.mirasalon.core.domain.repository.UnreadMessagesSource
import iz.mkao.mirasalon.core.domain.repository.UpcomingAppointmentsSource
import iz.mkao.mirasalon.feature.profile.domain.model.AppSettings
import iz.mkao.mirasalon.feature.profile.domain.model.NotificationPreferences
import iz.mkao.mirasalon.feature.profile.domain.repository.AddressRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.AppSettingsRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.NotificationPreferencesRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.ProfileRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.SessionController
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfilePresenter(
    private val profileRepository: ProfileRepository,
    private val addressRepository: AddressRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val notificationPreferencesRepository: NotificationPreferencesRepository,
    private val sessionController: SessionController,
    private val unreadMessagesSource: UnreadMessagesSource,
    private val upcomingAppointmentsSource: UpcomingAppointmentsSource,
    private val navigator: Navigator
) : Presenter<ProfileState> {

    @Composable
    override fun present(): ProfileState {
        val profileOutcome by profileRepository.observeProfile().collectAsState(initial = Outcome.Loading)
        val addresses by addressRepository.observeAddresses().collectAsState(initial = emptyList())
        val settings by appSettingsRepository.observeSettings().collectAsState(AppSettings())
        val notificationPreferences by notificationPreferencesRepository.observePreferences().collectAsState(initial = NotificationPreferences())
        val scope = rememberCoroutineScope()

        val profile = when (val outcome = profileOutcome) {
            is Outcome.Success -> outcome.data
            else -> null
        }
        val isLoading = profileOutcome is Outcome.Loading
        val error = null

        var unreadMessagesCount by remember { mutableStateOf(0) }
        var upcomingRemindersCount by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            profileRepository.getProfile()
            addressRepository.refresh()
            
            unreadMessagesSource.observeUnreadMessagesCount()
                .catch { emit(0) }
                .collect { unreadMessagesCount = it }
        }

        LaunchedEffect(Unit) {
            upcomingAppointmentsSource.observeUpcomingAppointmentsCount()
                .catch { emit(0) }
                .collect { upcomingRemindersCount = it }
        }

        val addressCount = addresses.size

        return ProfileState(
            isLoading = isLoading,
            profile = profile,
            addressCount = addressCount,
            unreadMessagesCount = unreadMessagesCount,
            upcomingRemindersCount = upcomingRemindersCount,
            currentTheme = settings.theme,
            inAppNotificationsEnabled = notificationPreferences.inAppEnabled,
            error = error,
            eventSink = { event ->
                when (event) {
                    ProfileEvent.EditProfile -> navigator.goTo(ProfileRoute.EditProfile)
                    ProfileEvent.SavedAddresses -> navigator.goTo(ProfileRoute.Addresses)
                    ProfileEvent.MyOrders -> navigator.goTo(CartRoute.Orders())
                    ProfileEvent.MyAppointments -> navigator.goTo(BottomNavKey.Booking())
                    ProfileEvent.Favourites -> navigator.goTo(ProfileRoute.Favourites)
                    ProfileEvent.PaymentMethods -> navigator.goTo(ProfileRoute.PaymentMethods)
                    ProfileEvent.Notifications -> navigator.goTo(NotificationRoute.Notifications)
                    is ProfileEvent.ToggleInAppNotifications -> {
                        scope.launch {
                            notificationPreferencesRepository.updatePreferences(
                                notificationPreferences.copy(inAppEnabled = event.enabled)
                            )
                        }
                    }
                    ProfileEvent.AppSettings -> {}
                    ProfileEvent.CurrencyAndTheme -> navigator.goTo(ProfileRoute.CurrencyAndTheme)
                    is ProfileEvent.SetTheme -> scope.launch { appSettingsRepository.setTheme(event.theme) }
                    ProfileEvent.Logout -> {
                        scope.launch {
                            sessionController.logout()
                            navigator.resetRoot(AuthRoute.Welcome)
                        }
                    }
                    ProfileEvent.Retry -> {} // Retry is automatic with collectAsState
                }
            }
        )
    }
}

class ProfileManualPresenterFactory(
    private val profileRepository: ProfileRepository,
    private val addressRepository: AddressRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val notificationPreferencesRepository: NotificationPreferencesRepository,
    private val sessionController: SessionController,
    private val unreadMessagesSource: UnreadMessagesSource,
    private val upcomingAppointmentsSource: UpcomingAppointmentsSource
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is BottomNavKey.Profile -> ProfilePresenter(
                profileRepository,
                addressRepository,
                appSettingsRepository,
                notificationPreferencesRepository,
                sessionController,
                unreadMessagesSource,
                upcomingAppointmentsSource,
                navigator
            )
            is ProfileRoute.EditProfile -> EditProfilePresenter(profileRepository, navigator)
            is ProfileRoute.Addresses -> AddressListPresenter(addressRepository, navigator)
            is ProfileRoute.AddressForm -> AddressFormPresenter(screen, addressRepository, profileRepository, navigator)
            is ProfileRoute.PaymentMethods -> PaymentMethodsPresenter(paymentMethodRepository, navigator)
            else -> null
        }
    }
}
