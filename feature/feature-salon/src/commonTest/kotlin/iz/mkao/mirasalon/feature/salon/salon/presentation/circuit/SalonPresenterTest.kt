package iz.mkao.mirasalon.feature.salon.salon.presentation.circuit

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import iz.mkao.mirasalon.core.domain.model.Salon
import iz.mkao.mirasalon.core.domain.model.SalonCategory
import iz.mkao.mirasalon.core.domain.model.SalonHome
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.SalonRepository
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.NotificationRoute
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.feature.profile.domain.model.Address
import iz.mkao.mirasalon.feature.profile.domain.model.ProfileUpdate
import iz.mkao.mirasalon.feature.profile.domain.model.UserProfile
import iz.mkao.mirasalon.feature.profile.domain.repository.AddressRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SalonPresenterTest {

    private val salonRepository = FakeSalonRepository()
    private val tokenProvider = FakeTokenProvider()
    private val profileRepository = FakeProfileRepository()
    private val addressRepository = FakeAddressRepository()
    private val navigator = FakeNavigator(BottomNavKey.Home())

    @Test
    fun presenter_initial_state_loads_data() = runTest {
        val presenter = SalonPresenter(
            repository = salonRepository,
            tokenProvider = tokenProvider,
            profileRepository = profileRepository,
            addressRepository = addressRepository,
            navigator = navigator
        )

        presenter.test {
            // Initial state
            var state = awaitItem()
            
            // Wait for loading to finish
            while (state.isLoading) {
                state = awaitItem()
            }
            
            assertEquals("Test User", state.userName)
            assertEquals("Test Street", state.userLocation)
            assertEquals(1, state.categories.size)
            assertEquals("Cat 1", state.categories[0].name)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun presenter_navigates_to_notifications() = runTest {
        val presenter = SalonPresenter(
            repository = salonRepository,
            tokenProvider = tokenProvider,
            profileRepository = profileRepository,
            addressRepository = addressRepository,
            navigator = navigator
        )

        presenter.test {
            var state = awaitItem()
            while (state.isLoading) {
                state = awaitItem()
            }
            
            state.eventSink(SalonEvent.NotificationClicked)
            assertEquals(NotificationRoute.Notifications, navigator.awaitNextScreen())
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeSalonRepository : SalonRepository {
        override suspend fun getHome(): Outcome<SalonHome> {
            return Outcome.Success(
                SalonHome(
                    categories = listOf(SalonCategory("1", "Cat 1", "")),
                    specialists = emptyList(),
                    promotions = emptyList(),
                    isLoggedIn = true
                )
            )
        }

        override suspend fun getSalon(id: String): Outcome<Salon> =
            Outcome.Error(Failure.Unknown)
    }

    private class FakeTokenProvider : SalonTokenProvider {
        override suspend fun accessToken(): String? = "token"
        override suspend fun refreshToken(): String? = "refresh"
        override suspend fun userId(): String? = "1"
        override suspend fun userName(): String? = "Test User"
        override suspend fun userAddress(): String? = "Test Street"
        override suspend fun userAvatarUrl(): String? = "url"
        override suspend fun savedEmail(): String? = "test@example.com"
        override suspend fun saveEmail(email: String?) {}
        override suspend fun onTokensRefreshed(accessToken: String, refreshToken: String, userId: String?, userName: String?, userAvatarUrl: String?, firstName: String?, lastName: String?, phone: String?, address: String?, gender: String?) {}
        override suspend fun onAuthenticationExpired() {}
    }

    private class FakeProfileRepository : ProfileRepository {
        private val profileFlow = MutableStateFlow<Outcome<UserProfile>>(Outcome.Loading)
        override fun observeProfile(): Flow<Outcome<UserProfile>> = profileFlow
        override suspend fun getProfile(): Outcome<UserProfile> = Outcome.Error(Failure.Unknown)
        override suspend fun updateProfile(patch: ProfileUpdate): Outcome<UserProfile> = Outcome.Error(
            Failure.Unknown)
        override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Outcome<String> = Outcome.Error(
            Failure.Unknown)
    }

    private class FakeAddressRepository : AddressRepository {
        override fun observeAddresses(): Flow<List<Address>> = flowOf(emptyList())
        override suspend fun addAddress(address: Address): Outcome<Address> = Outcome.Error(Failure.Unknown)
        override suspend fun updateAddress(address: Address): Outcome<Address> = Outcome.Error(
            Failure.Unknown)
        override suspend fun deleteAddress(id: String): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun setDefault(id: String): Outcome<Unit> = Outcome.Success(Unit)
    }
}
