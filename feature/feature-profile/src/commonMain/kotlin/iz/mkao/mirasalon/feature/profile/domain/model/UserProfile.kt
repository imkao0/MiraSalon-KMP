package iz.mkao.mirasalon.feature.profile.domain.model

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val gender: Gender? = null,
    val dateOfBirth: String? = null,
    val allergies: List<String>? = null,
    val memberSinceEpochSeconds: Long,
)
