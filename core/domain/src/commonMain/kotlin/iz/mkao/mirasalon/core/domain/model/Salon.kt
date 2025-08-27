package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable

data class Salon(
    val id: String,
    val name: String,
    val address: String,
    val imageUrl: String? = null,
    val rating: Double = 0.0,
    val distanceKm: Double? = null,
    val openTime: String = "08:00",
    val closeTime: String = "20:00",
    val timezoneId: String = "UTC"
)
