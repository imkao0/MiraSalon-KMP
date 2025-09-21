package iz.mkao.mirasalon.feature.auth.data.repository

import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.network.model.dto.*
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.feature.auth.data.network.api.AuthApi

class DefaultAuthRepository(
    private val authApi: AuthApi,
    private val tokenProvider: SalonTokenProvider
) : AuthRepository {

    override suspend fun login(request: LoginRequest): NetworkResult<AuthResponse> {
        val result = authApi.login(request)
        if (result is NetworkResult.Success) {
            tokenProvider.onTokensRefreshed(
                accessToken = result.data.token,
                refreshToken = result.data.refreshToken.orEmpty(),
                userId = result.data.userId,
                userName = result.data.name,
                userAvatarUrl = result.data.avatarUrl,
                firstName = result.data.firstName,
                lastName = result.data.lastName,
                phone = result.data.phone,
                address = result.data.address,
                gender = result.data.gender
            )
        }
        return result
    }

    override suspend fun register(request: RegisterRequest): NetworkResult<AuthResponse> {
        val result = authApi.register(request)
        if (result is NetworkResult.Success) {
            tokenProvider.onTokensRefreshed(
                accessToken = result.data.token,
                refreshToken = result.data.refreshToken.orEmpty(),
                userId = result.data.userId,
                userName = result.data.name,
                userAvatarUrl = result.data.avatarUrl,
                firstName = result.data.firstName,
                lastName = result.data.lastName,
                phone = result.data.phone,
                address = result.data.address,
                gender = result.data.gender
            )
        }
        return result
    }

    override suspend fun logout() {
        tokenProvider.onAuthenticationExpired()
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<Unit> {
        return authApi.updateProfile(request)
    }

    override suspend fun deleteAccount(): NetworkResult<Unit> {
        val result = authApi.deleteAccount()
        if (result is NetworkResult.Success) {
            logout()
        }
        return result
    }

    override suspend fun saveEmail(email: String?) {
        tokenProvider.saveEmail(email)
    }

    override suspend fun getSavedEmail(): String? {
        return tokenProvider.savedEmail()
    }
}