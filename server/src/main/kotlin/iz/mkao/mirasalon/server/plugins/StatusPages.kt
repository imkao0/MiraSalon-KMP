package iz.mkao.mirasalon.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.server.error.DomainException
import iz.mkao.mirasalon.server.error.ValidationException
import kotlinx.serialization.SerializationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC

private val log = LoggerFactory.getLogger("StatusPages")

fun Application.configureStatusPages() {
    install(StatusPages) {

        exception<DomainException> { call, cause ->
            call.respond(
                cause.status,
                ApiResponse<Unit>(success = false, error = cause.message)
            )
        }

        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                ApiResponse<Map<String, List<String>>>(success = false, error = cause.message, data = cause.errors)
            )
        }

        exception<NoSuchElementException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<Unit>(success = false, error = cause.message ?: "Not found")
            )
        }

        exception<Throwable> { call, cause ->
            val callId = MDC.get("callId") ?: "unknown"

            if (cause is JsonConvertException || cause is SerializationException) {
                log.warn("[StatusPages] Bad Request (Serialization) [callId=$callId]: ${cause.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Invalid request format: ${cause.message}")
                )
                return@exception
            }

            log.error("Unhandled exception [callId=$callId] on ${call.request.local.method} ${call.request.local.uri}", cause)

            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(success = false, error = "Internal server error")
            )
        }
    }
}
