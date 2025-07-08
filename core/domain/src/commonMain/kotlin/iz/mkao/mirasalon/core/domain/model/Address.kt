package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable

data class Address(
    val id: String = "",
    val name: String = "",
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
    val isDefault: Boolean = false
)
