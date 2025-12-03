package iz.mkao.mirasalon.core.domain.model

/** Lightweight customer row for list screens. */
data class CustomerSummary(
    val id: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val imageUrl: String? = null,
    val totalBookings: Int = 0,
    val totalSpent: Double = 0.0,
    val lastVisit: Long? = null,
)

/** Full customer profile for the detail/edit dialog. */
data class CustomerDetail(
    val id: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val imageUrl: String? = null,
    val address: String = "",
    val gender: String = "",
    val createdAt: Long = 0L,
    val totalBookings: Int = 0,
    val totalSpent: Double = 0.0,
)
