package iz.mkao.mirasalon.core.domain.model

/**
 * Salon profile as managed by the desktop admin app.
 *
 * This is the shared domain representation of a salon branch; network DTOs are
 * mapped to this model in the data layer so UI code never touches DTOs.
 */
data class AdminSalon(
    val id: String,
    val name: String,
    val address: String = "",
    val imageUrl: String? = null,
    val phone: String? = null,
    val rating: Double = 0.0,
    val openTime: String? = null,
    val closeTime: String? = null,
    val timezoneId: String? = "UTC"
)
