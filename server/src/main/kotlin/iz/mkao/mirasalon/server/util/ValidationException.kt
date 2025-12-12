package iz.mkao.mirasalon.server.util

data class ValidationException(
    override val message: String,
    val errors: Map<String, List<String>> = emptyMap()
) : RuntimeException(message)
