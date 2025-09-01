package iz.mkao.mirasalon.core.network.client.admin

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.*
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorAdminBookingsApi(private val httpClient: HttpClient) : AdminBookingsApi {

    override suspend fun fetchBookings(
        status: String?,
        specialistId: String?,
        query: String?,
        dateFrom: Long?,
        dateTo: Long?,
        page: Int,
        pageSize: Int
    ): Outcome<PagedResponse<AppointmentDto>> = apiCall {
        httpClient.get(Endpoints.BOOKINGS) {
            parameter("status", status)
            parameter("specialistId", specialistId)
            parameter("query", query)
            parameter("dateFrom", dateFrom)
            parameter("dateTo", dateTo)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }
    }

    override suspend fun updateBookingStatus(id: String, status: String): Outcome<AppointmentDto> = apiCall {
        httpClient.put(Endpoints.bookingStatus(id)) {
            contentType(ContentType.Application.Json)
            setBody(UpdateAppointmentStatusRequest(status))
        }
    }

    override suspend fun deleteBooking(id: String): Outcome<Unit> = apiCall {
        httpClient.delete(Endpoints.bookingDetail(id))
    }

    override suspend fun createBooking(request: CreateAppointmentRequest): Outcome<AppointmentDto> = apiCall {
        httpClient.post(Endpoints.BOOKINGS) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    private object Endpoints {
        const val BOOKINGS = "/v1/api/bookings"
        fun bookingStatus(id: String) = "/v1/api/bookings/$id/status"
        fun bookingDetail(id: String) = "/v1/api/bookings/$id"
    }
}
