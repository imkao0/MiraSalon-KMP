package iz.mkao.mirasalon.core.network.util

import io.github.aakira.napier.Napier
import io.ktor.client.plugins.logging.Logger

object SalonNetworkLogger : Logger {
    override fun log(message: String) {
        Napier.d(tag = "Network") { message }
    }
}
