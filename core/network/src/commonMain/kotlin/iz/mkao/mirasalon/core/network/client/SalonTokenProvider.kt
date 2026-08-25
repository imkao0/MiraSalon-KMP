package iz.mkao.mirasalon.core.network.client

import kotlinx.coroutines.flow.Flow

interface SalonTokenProvider {
    suspend fun accessToken(): String?
    suspend fun refreshToken(): String?
    suspend fun userId(): String?
    suspend fun userName(): String?
    suspend fun userAddress(): String?
    suspend fun userAvatarUrl(): String?
    suspend fun savedEmail(): String?
    suspend fun saveEmail(email: String?)
    suspend fun onTokensRefreshed(
        accessToken: String,
        refreshToken: String,
        userId: String? = null,
        userName: String? = null,
        userAvatarUrl: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        phone: String? = null,
        address: String? = null,
        gender: String? = null
    )
    suspend fun onAuthenticationExpired()

    fun observeUserId(): Flow<String?>
}
