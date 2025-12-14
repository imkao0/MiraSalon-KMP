package iz.mkao.mirasalon.server.storage

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class UploadResult(
    val url: String,
    val fileName: String,
    val sizeBytes: Long,
    val contentType: String
)

interface StorageService {
    suspend fun uploadStream(
        stream: () -> ByteReadChannel,
        key: String,
        originalFileName: String,
        contentType: String
    ): UploadResult
}

class LocalStorageService(private val uploadDir: String) : StorageService {
    init {
        File(uploadDir).mkdirs()
    }

    override suspend fun uploadStream(
        stream: () -> ByteReadChannel,
        key: String,
        originalFileName: String,
        contentType: String
    ): UploadResult {
        val targetFile = File(uploadDir, key)
        targetFile.parentFile.mkdirs()
        
        val inputStream = stream().toInputStream()
        Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        
        return UploadResult(
            url = "/uploads/$key",
            fileName = originalFileName,
            sizeBytes = targetFile.length(),
            contentType = contentType
        )
    }
}
