package iz.mkao.mirasalon.server.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.ktor.ext.inject

fun Application.configureMetrics() {
    val registry by inject<PrometheusMeterRegistry>()

    install(MicrometerMetrics) {
        this.registry = registry
    }

    routing {
        authenticate("metrics-auth") {
            get("/metrics") {
                val response = registry.scrape()
                call.respond(response)
            }
        }
    }
}
