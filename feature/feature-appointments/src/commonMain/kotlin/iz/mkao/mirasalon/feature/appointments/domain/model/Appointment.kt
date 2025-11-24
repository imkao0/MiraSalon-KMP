package iz.mkao.mirasalon.feature.appointments.domain.model

import iz.mkao.mirasalon.core.domain.model.Service
import kotlinx.datetime.LocalDate

data class Appointment(
    val id: String,
    val salonName: String,
    val specialistName: String,
    val specialistId: String,
    val dateTime: Long,

    val services: List<Service>,
    val status: AppointmentStatus,
    val totalAmount: Double,
)

enum class AppointmentStatus {
    Confirmed,
    Completed,
    Cancelled
}
