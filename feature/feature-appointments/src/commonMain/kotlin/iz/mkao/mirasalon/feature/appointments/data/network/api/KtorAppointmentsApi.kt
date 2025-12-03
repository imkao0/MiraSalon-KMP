package iz.mkao.mirasalon.feature.appointments.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.AppointmentDto
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.network.result.safeApiCall

class KtorAppointmentsApi(private val httpClient: HttpClient) : AppointmentsApi {
    override suspend fun fetchAppointments(): NetworkResult<PagedResponse<AppointmentDto>> = safeApiCall {
        httpClient.get("/v1/api/bookings")
    }

    override suspend fun fetchAppointmentById(id: String): NetworkResult<AppointmentDto> = safeApiCall {
        httpClient.get("/v1/api/bookings/$id")
    }
}
