package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable

data class ServiceCategory(
    val id: String,
    val name: String,
    val iconName: String? = null,
    val iconUrl: String? = null
)
