package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.AdminAppointment
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStatus
import iz.mkao.mirasalon.core.domain.model.CreateAppointment
import iz.mkao.mirasalon.core.domain.outcome.Outcome

interface BookingsRepository {
    suspend fun getAll(
        status: AdminAppointmentStatus? = null,
        query: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null
    ): Outcome<List<AdminAppointment>>
    
    suspend fun updateStatus(id: String, status: AdminAppointmentStatus): Outcome<Unit>
    suspend fun delete(id: String): Outcome<Unit>
    suspend fun create(request: CreateAppointment): Outcome<Unit>
}
