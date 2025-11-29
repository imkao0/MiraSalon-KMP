package iz.mkao.mirasalon.core.network.model

data class SalonApiConfig(
    val baseUrl: String,
    val webSocketUrl: String,
    val streamApiKey: String? = null,
    val streamApiSecret: String? = null,
    val requestTimeoutMillis: Long = 30_000L,
    val connectTimeoutMillis: Long = 15_000L,
    val socketTimeoutMillis: Long = 30_000L,
    val maxRetries: Int = 2,
    val enableLogging: Boolean = false,
)
