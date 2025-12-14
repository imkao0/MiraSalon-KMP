package iz.mkao.mirasalon.server.util

data class AppConfig(
    val streamApiKey: String,
    val streamApiSecret: String,
    val streamAppId: String,
    val uploadDir: String,
    val environment: String
)
