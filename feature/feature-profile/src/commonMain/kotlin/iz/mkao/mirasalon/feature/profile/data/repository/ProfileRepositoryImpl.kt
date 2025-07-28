package iz.mkao.mirasalon.feature.profile.data.repository

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.feature.profile.data.dto.ProfileUpdateRequest
import iz.mkao.mirasalon.feature.profile.data.mapper.toDomain
import iz.mkao.mirasalon.feature.profile.data.mapper.toFailure
import iz.mkao.mirasalon.feature.profile.data.remote.ProfileApi
import iz.mkao.mirasalon.feature.profile.domain.model.ProfileUpdate
import iz.mkao.mirasalon.feature.profile.domain.model.UserProfile
import iz.mkao.mirasalon.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProfileRepositoryImpl(
    private val api: ProfileApi,
    private val tokenProvider: SalonTokenProvider,
    private val realtimeGateway: RealtimeGateway,
    private val repositoryScope: CoroutineScope
) : ProfileRepository {

    private val _profileCache = MutableStateFlow<Outcome<UserProfile>?>(null)

    init {
        observeRealtimeEvents()
        refreshProfile()
    }

    private fun observeRealtimeEvents() {
        realtimeGateway.events
            .filterIsInstance<DomainEvent.UserProfileUpdated>()
            .onEach { refreshProfile() }
            .launchIn(repositoryScope)
    }

    private fun refreshProfile() {
        repositoryScope.launch {
            _profileCache.value = getProfileFromNetwork()
        }
    }

    override fun observeProfile(): Flow<Outcome<UserProfile>> =
        _profileCache.filterNotNull()

    override suspend fun getProfile(): Outcome<UserProfile> = getProfileFromNetwork().also {
        _profileCache.value = it
    }

    private suspend fun getProfileFromNetwork(): Outcome<UserProfile> =
        when (val result = api.fetchProfile()) {
            is NetworkResult.Success -> Outcome.Success(result.data.toDomain())
            is NetworkResult.Error -> Outcome.Error(result.error.toFailure())
        }

    override suspend fun updateProfile(patch: ProfileUpdate): Outcome<UserProfile> {
        val request = ProfileUpdateRequest(
            fullName = patch.fullName,
            phoneNumber = patch.phoneNumber,
            gender = patch.gender?.name,
            dateOfBirth = patch.dateOfBirth,
            allergies = patch.allergies,
        )
        val result = when (val networkResult = api.updateProfile(request)) {
            is NetworkResult.Success -> Outcome.Success(networkResult.data.toDomain())
            is NetworkResult.Error -> Outcome.Error(networkResult.error.toFailure())
        }
        if (result is Outcome.Success) {
            _profileCache.value = result
        }
        return result
    }

    override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Outcome<String> =
        when (val result = api.uploadAvatar(bytes, mimeType)) {
            is NetworkResult.Success -> {
                val newAvatarUrl = result.data.avatarUrl
                // Update token provider so other screens (like SalonScreen) get the new URL
                tokenProvider.onTokensRefreshed(
                    accessToken = tokenProvider.accessToken().orEmpty(),
                    refreshToken = tokenProvider.refreshToken().orEmpty(),
                    userAvatarUrl = newAvatarUrl
                )
                refreshProfile() // Refresh profile to get updated avatar URL in observeProfile()
                Outcome.Success(newAvatarUrl)
            }
            is NetworkResult.Error -> Outcome.Error(result.error.toFailure())
        }
}
