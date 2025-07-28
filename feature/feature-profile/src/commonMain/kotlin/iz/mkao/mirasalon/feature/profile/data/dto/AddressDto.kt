package iz.mkao.mirasalon.feature.profile.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddressDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val label: String,
    val phoneNumber: String,
    val streetAddress: String,
    val number: String,
    val city: String,
    val state: String,
    val isDefault: Boolean = false,
)
