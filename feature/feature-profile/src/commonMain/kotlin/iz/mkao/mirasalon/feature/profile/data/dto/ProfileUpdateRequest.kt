package iz.mkao.mirasalon.feature.profile.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpdateRequest(
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val allergies: List<String>? = null,
)
