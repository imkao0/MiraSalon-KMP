package iz.mkao.mirasalon.core.common.analytics

import io.github.aakira.napier.Napier

interface AnalyticsLogger {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
}

class NapierAnalyticsLogger : AnalyticsLogger {
    override fun logEvent(name: String, params: Map<String, String>) {
        Napier.d(tag = "Analytics") { "Event: $name, Params: $params" }
    }
}