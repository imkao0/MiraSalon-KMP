package iz.mkao.mirasalon.feature.auth.data.network.api

import iz.mkao.mirasalon.core.network.model.dto.*
import iz.mkao.mirasalon.core.network.result.NetworkResult

interface AuthApi {
    suspend fun login(request: LoginRequest): NetworkResult<AuthResponse>
    suspend fun register(request: RegisterRequest): NetworkResult<AuthResponse>
    suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<Unit>
    suspend fun deleteAccount(): NetworkResult<Unit>
}
