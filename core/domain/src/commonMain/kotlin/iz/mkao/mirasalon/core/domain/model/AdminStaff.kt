package iz.mkao.mirasalon.core.domain.model

/**
 * Admin-facing specialist (staff member) model.
 */
data class AdminSpecialist(
    val id: String,
    val salonId: String = "",
    val name: String,
    val role: String = "",
    val email: String = "",
    val phone: String = "",
    val imageUrl: String? = null,
    val rating: Double = 0.0,
    val isAvailable: Boolean = true,
    val isActive: Boolean = true,
    val bio: String = "",
    val customersServed: Int = 0,
    val yearsOfExperience: Int = 0,
    val services: List<Service> = emptyList(),
    val shifts: List<AdminSpecialistShift> = emptyList()
)

/** A recurring weekly shift for a specialist. */
data class AdminSpecialistShift(
    val id: String = "",
    val specialistId: String = "",
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val isActive: Boolean = true
)

/** Aggregated performance statistics for a specialist. */
data class AdminSpecialistStats(
    val specialistId: String = "",
    val specialistName: String = "",
    val totalAppointments: Int = 0,
    val completedAppointments: Int = 0,
    val cancelledAppointments: Int = 0,
    val revenue: Double = 0.0,
    val revenueGrowth: Double = 0.0,
    val averageRating: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val serviceCount: Int = 0
)
