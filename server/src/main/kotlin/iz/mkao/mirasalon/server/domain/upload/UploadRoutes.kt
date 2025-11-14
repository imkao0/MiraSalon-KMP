package iz.mkao.mirasalon.server.domain.upload

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.UploadResponse
import iz.mkao.mirasalon.server.storage.StorageService
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger("UploadRoutes")

fun Route.uploadRoutes(storageService: StorageService) {

    authenticate("auth-jwt") {

        // ── Image Upload ──
        post("/image") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asString()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val allowedTypes = setOf("image/jpeg", "image/png", "image/webp")
            val allowedExtensions = setOf("jpg", "jpeg", "png", "webp")

            try {
                val multipart = call.receiveMultipart()
                var uploadResult: iz.mkao.mirasalon.server.storage.UploadResult? = null

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem && uploadResult == null) {
                        val originalFileName = part.originalFileName ?: "upload"
                        val contentType = part.contentType?.toString() ?:
                            "application/octet-stream"
                        val ext = originalFileName.substringAfterLast('.', "").lowercase()

                        if (ext in allowedExtensions && contentType in allowedTypes) {
                            val key = "images/${UUID.randomUUID()}.$ext"
                            uploadResult = storageService.uploadStream(
                                stream = part.provider(),
                                key = key,
                                originalFileName = originalFileName,
                                contentType = contentType
                            )
                        }
                    }
                    part.release
                }

                val result = uploadResult ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "No valid file provided")
                )

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = UploadResponse(
                            url = result.url,
                            fileName = result.fileName,
                            sizeBytes = result.sizeBytes,
                            contentType = result.contentType
                        )
                    )
                )

            } catch (e: Exception) {
                log.error("Upload error", e)
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
            }
        }
    }
}
