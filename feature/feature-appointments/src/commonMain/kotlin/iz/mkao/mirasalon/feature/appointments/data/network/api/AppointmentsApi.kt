package iz.mkao.mirasalon.feature.appointments.data.network.api

import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.AppointmentDto
import iz.mkao.mirasalon.core.network.result.NetworkResult

interface AppointmentsApi {
    suspend fun fetchAppointments(): NetworkResult<PagedResponse<AppointmentDto>>
    suspend fun fetchAppointmentById(id: String): NetworkResult<AppointmentDto>
}
