package iz.mkao.mirasalon.server.plugins

import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

/**
 * CORS is locked down to explicitly allowed origins read from the CORS_ALLOWED_ORIGINS env var.
 * Falls back to localhost for local development.
 *
 * Set CORS_ALLOWED_ORIGINS=https://app.mirasalon.com,https://admin.mirasalon.com in production.
 */
fun Application.configureCORS() {
    val allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS")
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: listOf("http://localhost:3000", "http://localhost:8080", "http://127.0.0.1:8080", "http://192.168.1.113:8080", "http://10.0.2.2:8080")

    install(CORS) {
        allowedOrigins.forEach { origin ->
            if (origin.contains("://")) {
                val host = origin.substringAfter("://")
                val scheme = origin.substringBefore("://")
                allowHost(host, schemes = listOf(scheme))
            } else {
                allowHost(origin)
            }
        }

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)

        allowHeader("Authorization")
        allowHeader("Content-Type")
        allowHeader("X-Request-ID")
        allowHeader("Idempotency-Key")

        allowCredentials = true

        maxAgeInSeconds = 3600
    }
}
