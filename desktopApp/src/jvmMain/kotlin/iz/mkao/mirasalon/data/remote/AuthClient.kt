package iz.mkao.mirasalon.data.remote

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.AuthResponse
import iz.mkao.mirasalon.core.network.model.dto.LoginRequest
import iz.mkao.mirasalon.core.common.result.NetworkResult
import iz.mkao.mirasalon.core.network.util.safeApiCall
import iz.mkao.mirasalon.core.network.util.safeEmptyApiCall
import kotlinx.coroutines.Dispatchers

class AuthClient(private val client: HttpClient, private val tokenManager: iz.mkao.mirasalon.data.local.TokenManager) {

    suspend fun login(email: String, password: String): NetworkResult<AuthResponse> = safeApiCall(Dispatchers.IO) {
        client.post("/v1/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }
    }

    suspend fun getProfile(): NetworkResult<AuthResponse> = safeApiCall(Dispatchers.IO) {
        client.get("/v1/api/auth/profile") {
            header(HttpHeaders.Authorization, tokenManager.getAuthHeader())
        }
    }

    suspend fun updateProfile(
        firstName: String? = null,
        lastName: String? = null,
        phone: String? = null,
        address: String? = null,
        gender: String? = null,
        avatarUrl: String? = null
    ): NetworkResult<Unit> = safeEmptyApiCall(Dispatchers.IO) {
        client.put("/v1/api/auth/profile") {
            header(HttpHeaders.Authorization, tokenManager.getAuthHeader())
            contentType(ContentType.Application.Json)
            setBody(
                iz.mkao.mirasalon.core.network.model.dto.UpdateProfileRequest(
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone,
                    address = address,
                    gender = gender,
                    avatarUrl = avatarUrl
                )
            )
        }
    }
}
