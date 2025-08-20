package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable

data class SalonCategory(
    val id: String,
    val name: String,
    val iconName: String? = null,
)
