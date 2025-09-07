package iz.mkao.mirasalon.feature.appointments.domain.repository

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.feature.appointments.domain.model.Appointment
import kotlinx.coroutines.flow.Flow

interface AppointmentRepository {
    fun observeAppointments(): Flow<Outcome<List<Appointment>>>
    suspend fun refreshAppointments()
    suspend fun cancelAppointment(id: String): Outcome<Unit>
}
