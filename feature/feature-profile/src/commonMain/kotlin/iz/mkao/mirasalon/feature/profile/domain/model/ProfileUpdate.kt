package iz.mkao.mirasalon.feature.profile.domain.model

data class ProfileUpdate(
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val gender: Gender? = null,
    val dateOfBirth: String? = null,
    val allergies: List<String>? = null,
)
