package iz.mkao.mirasalon.feature.auth.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.network.model.dto.*
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.network.result.safeApiCall

class KtorAuthApi(private val httpClient: HttpClient) : AuthApi {

    override suspend fun login(request: LoginRequest): NetworkResult<AuthResponse> = safeApiCall {
        httpClient.post("/v1/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun register(request: RegisterRequest): NetworkResult<AuthResponse> = safeApiCall {
        httpClient.post("/v1/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<Unit> = safeApiCall {
        httpClient.put("/v1/api/auth/profile") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun deleteAccount(): NetworkResult<Unit> = safeApiCall {
        httpClient.delete("/v1/api/auth/profile")
    }
}