package iz.mkao.mirasalon.core.network.util

import kotlinx.serialization.json.Json

val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = true
}
