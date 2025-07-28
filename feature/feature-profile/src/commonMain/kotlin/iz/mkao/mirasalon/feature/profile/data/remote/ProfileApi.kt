package iz.mkao.mirasalon.feature.profile.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.network.result.safeApiCall
import iz.mkao.mirasalon.feature.profile.data.dto.AddressDto
import iz.mkao.mirasalon.feature.profile.data.dto.AvatarUploadResponse
import iz.mkao.mirasalon.feature.profile.data.dto.NotificationPreferencesDto
import iz.mkao.mirasalon.feature.profile.data.dto.ProfileUpdateRequest
import iz.mkao.mirasalon.feature.profile.data.dto.UserProfileDto

class ProfileApi(private val httpClient: HttpClient) {

    suspend fun fetchProfile(): NetworkResult<UserProfileDto> = safeApiCall {
        httpClient.get(Endpoints.PROFILE)
    }

    suspend fun updateProfile(request: ProfileUpdateRequest): NetworkResult<UserProfileDto> = safeApiCall {
        httpClient.put(Endpoints.PROFILE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun fetchNotificationPreferences(): NetworkResult<NotificationPreferencesDto> = safeApiCall {
        httpClient.get(Endpoints.NOTIFICATION_PREFERENCES)
    }

    suspend fun updateNotificationPreferences(preferences: NotificationPreferencesDto): NetworkResult<NotificationPreferencesDto> = safeApiCall {
        httpClient.put(Endpoints.NOTIFICATION_PREFERENCES) {
            contentType(ContentType.Application.Json)
            setBody(preferences)
        }
    }

    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): NetworkResult<AvatarUploadResponse> = safeApiCall {
        httpClient.submitFormWithBinaryData(
            url = Endpoints.AVATAR,
            formData = formData {
                append(
                    key = "avatar",
                    value = bytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"avatar\"")
                    },
                )
            },
        )
    }

    suspend fun fetchAddresses(): NetworkResult<List<AddressDto>> = safeApiCall {
        httpClient.get(Endpoints.ADDRESSES)
    }

    suspend fun addAddress(address: AddressDto): NetworkResult<AddressDto> = safeApiCall {
        httpClient.post(Endpoints.ADDRESSES) {
            contentType(ContentType.Application.Json)
            setBody(address)
        }
    }

    suspend fun updateAddress(address: AddressDto): NetworkResult<AddressDto> = safeApiCall {
        httpClient.put(Endpoints.address(address.id)) {
            contentType(ContentType.Application.Json)
            setBody(address)
        }
    }

    suspend fun deleteAddress(id: String): NetworkResult<Unit> = safeApiCall {
        httpClient.delete(Endpoints.address(id))
    }

    suspend fun setDefaultAddress(id: String): NetworkResult<Unit> = safeApiCall {
        httpClient.post(Endpoints.setDefaultAddress(id))
    }

    private object Endpoints {
        const val PROFILE = "/v1/api/profile"
        const val AVATAR = "/v1/api/profile/avatar"
        const val ADDRESSES = "/v1/api/profile/addresses"
        const val NOTIFICATION_PREFERENCES = "/v1/api/profile/notification-preferences"
        fun address(id: String) = "/v1/api/profile/addresses/$id"
        fun setDefaultAddress(id: String) = "/v1/api/profile/addresses/$id/default"
    }
}
