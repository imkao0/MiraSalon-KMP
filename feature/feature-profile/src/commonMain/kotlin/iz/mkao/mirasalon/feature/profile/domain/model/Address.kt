package iz.mkao.mirasalon.feature.profile.domain.model

data class Address(
    val id: String,
    val firstName: String,
    val lastName: String,
    val label: AddressLabel,
    val phoneNumber: String,
    val streetAddress: String,
    val number: String,
    val city: String,
    val state: String,
    val zipCode: String = "",
    val country: String = "",
    val isDefault: Boolean = false,
)
