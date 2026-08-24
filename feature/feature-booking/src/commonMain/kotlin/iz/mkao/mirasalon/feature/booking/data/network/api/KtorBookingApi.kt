package iz.mkao.mirasalon.feature.booking.data.network.api

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
import iz.mkao.mirasalon.core.network.model.dto.AppointmentDto
import iz.mkao.mirasalon.core.network.model.dto.CreateAppointmentRequest
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto
import iz.mkao.mirasalon.core.network.model.dto.SalonDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceCategoryDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistAvailabilityDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistDto
import iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorBookingApi(private val httpClient: HttpClient) : BookingApi {

    override suspend fun fetchAvailability(
        specialistId: String,
        date: String,
        duration: Int?
    ): Outcome<SpecialistAvailabilityDto> = apiCall {
        httpClient.get("/v1/api/specialists/$specialistId/available-slots") {
            parameter("date", date)
            duration?.let { parameter("duration", it) }
        }
    }

    override suspend fun fetchSpecialists(): Outcome<List<SpecialistDto>> =
        apiCall {
            httpClient.get("/v1/api/specialists")
        }

    override suspend fun fetchServices(
        categoryId: String?,
        query: String?
    ): Outcome<List<ServiceDto>> = apiCall {
        httpClient.get("/v1/api/services") {
            parameter("categoryId", categoryId)
            parameter("query", query)
        }
    }

    override suspend fun fetchServicesCategories(): Outcome<List<ServiceCategoryDto>> = apiCall {
        httpClient.get("/v1/api/services/categories")
    }

    override suspend fun fetchSalons(): Outcome<PagedResponse<SalonDto>> = apiCall {
        httpClient.get("/v1/api/salons") {
            parameter("page", 1)
            parameter("pageSize", 1)
        }
    }

    override suspend fun createAppointment(request: CreateAppointmentRequest): Outcome<AppointmentDto> = apiCall {
        httpClient.post("/v1/api/bookings") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun fetchAppointments(): Outcome<PagedResponse<AppointmentDto>> = apiCall {
        httpClient.get("/v1/api/bookings")
    }

    override suspend fun cancelAppointment(id: String): Outcome<Unit> = apiCall {
        httpClient.delete("/v1/api/bookings/$id")
    }

    override suspend fun submitReview(bookingId: String, request: SubmitReviewRequest): Outcome<Unit> = apiCall<ReviewDto> {
        httpClient.post("/v1/api/reviews") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }.map { Unit }

    override suspend fun updateReminderEnabled(bookingId: String, enabled: Boolean): Outcome<Unit> = apiCall {
        httpClient.put("/v1/api/bookings/$bookingId/reminder") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("enabled" to enabled))
        }
    }
}
