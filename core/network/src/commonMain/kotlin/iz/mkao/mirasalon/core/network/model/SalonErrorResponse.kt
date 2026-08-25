package iz.mkao.mirasalon.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SalonErrorResponse(
    val message: String? = null,
    val errors: Map<String, String>? = null,
)
