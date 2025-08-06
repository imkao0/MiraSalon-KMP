package iz.mkao.mirasalon.feature.profile.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val allergies: List<String>? = null,
    val memberSinceEpochSeconds: Long,
)
