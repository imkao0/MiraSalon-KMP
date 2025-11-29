package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.outcome.Outcome

interface UploadRepository {
    suspend fun uploadImage(bytes: ByteArray, fileName: String): Outcome<String>

    /**
     * Uploads an image via the generic image upload endpoint, preserving the original
     * MIME type. Used for chat image attachments (jpg, png, webp).
     */
    suspend fun uploadImage(bytes: ByteArray, fileName: String, mimeType: String): Outcome<String> =
        uploadImage(bytes, fileName)
}
