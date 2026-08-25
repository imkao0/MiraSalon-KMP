package iz.mkao.mirasalon.feature.auth.data.repository

import iz.mkao.mirasalon.core.network.model.dto.*
import iz.mkao.mirasalon.core.network.result.NetworkResult

interface AuthRepository {
    suspend fun login(request: LoginRequest): NetworkResult<AuthResponse>
    suspend fun register(request: RegisterRequest): NetworkResult<AuthResponse>
    suspend fun logout()
    suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<Unit>
    suspend fun deleteAccount(): NetworkResult<Unit>
    suspend fun saveEmail(email: String?)
    suspend fun getSavedEmail(): String?
}

