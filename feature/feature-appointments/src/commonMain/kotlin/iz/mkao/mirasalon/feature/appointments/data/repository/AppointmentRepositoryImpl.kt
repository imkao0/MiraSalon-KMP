package iz.mkao.mirasalon.feature.appointments.data.repository

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.feature.appointments.domain.model.Appointment
import iz.mkao.mirasalon.feature.appointments.domain.model.AppointmentStatus
import iz.mkao.mirasalon.feature.appointments.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class AppointmentRepositoryImpl : AppointmentRepository {
    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())

    override fun observeAppointments(): Flow<Outcome<List<Appointment>>> {
        return _appointments.map { Outcome.Success(it) }
    }

    override suspend fun refreshAppointments() {

    }

    override suspend fun cancelAppointment(id: String): Outcome<Unit> {
        _appointments.value = _appointments.value.map {
            if (it.id == id) it.copy(status = AppointmentStatus.Cancelled) else it
        }
        return Outcome.Success(Unit)
    }
}
