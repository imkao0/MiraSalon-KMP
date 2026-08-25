package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
public enum class SpecialistStatus {
    ONLINE,
    OFFLINE,
    AWAY,
    BUSY
}

@Serializable
public data class Specialist(
    val id: String,
    val name: String,
    val role: String,
    val salonId: String = "",
    val rating: Double = 0.0,
    val imageUrl: String? = null,
    val isOnline: Boolean = false,
    val isVerified: Boolean = false,
    val bio: String = "",
    val customersCount: Int = 0,
    val yearsOfExperience: Int = 0,
    val services: List<Service> = emptyList(),
    val reviews: List<SpecialistReview> = emptyList(),
    val isActive: Boolean = true,
    val status: SpecialistStatus = SpecialistStatus.OFFLINE,
    val userId: String? = null
)
