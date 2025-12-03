package iz.mkao.mirasalon.core.network.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.UploadRepository
import iz.mkao.mirasalon.core.network.model.dto.AvatarUploadResponse
import iz.mkao.mirasalon.core.network.model.dto.UploadResponse
import iz.mkao.mirasalon.core.network.result.apiCall

class DefaultUploadRepository(private val client: HttpClient) : UploadRepository {
    override suspend fun uploadImage(bytes: ByteArray, fileName: String): Outcome<String> {
        return apiCall<AvatarUploadResponse> {
            client.post("/v1/api/auth/profile/avatar") {
                setBody(MultiPartFormDataContent(
                    formData {
                        append("avatar", bytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }
                ))
            }
        }.map { it.avatarUrl }
    }

    override suspend fun uploadImage(bytes: ByteArray, fileName: String, mimeType: String): Outcome<String> {
        return apiCall<UploadResponse> {
            client.post("/v1/api/upload/image") {
                setBody(MultiPartFormDataContent(
                    formData {
                        append("file", bytes, Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }
                ))
            }
        }.map { it.url }
    }
}
