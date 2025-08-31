package iz.mkao.mirasalon.core.domain.model
import kotlin.time.Clock
enum class AdminAppointmentStatus {
    Confirmed,
    Completed,
    Cancelled,
    ;

    companion object {
        fun fromString(value: String?): AdminAppointmentStatus =
            entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: Confirmed
    }
}

/**
 * Admin-facing appointment (booking) model.
 */
data class AdminAppointment(
    val id: String,
    val customerId: String = "",
    val customerName: String = "",
    val specialistId: String = "",
    val specialistName: String = "",
    val salonId: String = "",
    val salonName: String = "",
    val serviceIds: List<String> = emptyList(),
    val serviceNames: List<String> = emptyList(),
    val dateTime: Long,
    val durationMinutes: Int = 30,
    val totalAmount: Double = 0.0,
    val status: AdminAppointmentStatus = AdminAppointmentStatus.Confirmed,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)

data class CreateAppointment(
    val salonId: String,
    val userId: String,
    val specialistId: String,
    val dateTime: Long,
    val serviceIds: List<String>,
    val promoCode: String? = null
)

/**
 * Aggregated booking statistics for the dashboard.
 */
data class AdminAppointmentStats(
    val total: Int = 0,
    val confirmed: Int = 0,
    val completed: Int = 0,
    val cancelled: Int = 0,
    val revenue: Double = 0.0,
    val appointmentRevenue: Double = 0.0,
    val productRevenue: Double = 0.0,
    val revenueGrowth: Double = 0.0,
    val points: List<AppointmentDailyPoint> = emptyList()
)

data class AppointmentDailyPoint(
    val date: String,
    val returningClients: Int = 0,
    val newClients: Int = 0,
    val confirmed: Int = 0,
    val cancelled: Int = 0
)
