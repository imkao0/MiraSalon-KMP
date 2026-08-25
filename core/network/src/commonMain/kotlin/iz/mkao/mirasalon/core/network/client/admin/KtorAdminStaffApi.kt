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
import iz.mkao.mirasalon.core.network.model.dto.*
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorAdminStaffApi(private val httpClient: HttpClient) : AdminStaffApi {

    override suspend fun fetchStaff(query: String?): Outcome<List<SpecialistDto>> = apiCall<List<SpecialistDto>> {
        httpClient.get(Endpoints.STAFF) {
            parameter("query", query)
        }
    }

    override suspend fun createStaff(request: CreateSpecialistRequestDto): Outcome<SpecialistDto> = apiCall<SpecialistDto> {
        httpClient.post(Endpoints.STAFF) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun updateStaff(id: String, request: UpdateSpecialistRequestDto): Outcome<Unit> = apiCall<Unit> {
        httpClient.put(Endpoints.staff(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun updateStaffStatus(id: String, status: String): Outcome<Unit> = apiCall<Unit> {
        httpClient.put(Endpoints.staffStatus(id)) {
            contentType(ContentType.Application.Json)
            setBody(UpdateSpecialistStatusRequest(status))
        }
    }

    override suspend fun updateStaffActive(id: String, isActive: Boolean): Outcome<Unit> = apiCall<Unit> {
        httpClient.put(Endpoints.staffActive(id)) {
            contentType(ContentType.Application.Json)
            setBody(UpdateSpecialistActiveRequest(isActive))
        }
    }

    override suspend fun fetchStaffStats(id: String): Outcome<SpecialistPerformanceDto> = apiCall<SpecialistPerformanceDto> {
        httpClient.get(Endpoints.staffStats(id))
    }

    override suspend fun updateStaffShifts(id: String, shifts: List<SpecialistShiftDto>): Outcome<Unit> = apiCall<Unit> {
        httpClient.post(Endpoints.staffShifts(id)) {
            contentType(ContentType.Application.Json)
            setBody(shifts)
        }
    }

    override suspend fun deleteStaff(id: String): Outcome<Unit> = apiCall<Unit> {
        httpClient.delete(Endpoints.staff(id))
    }

    private object Endpoints {
        const val STAFF = "/v1/api/specialists"
        fun staff(id: String) = "/v1/api/specialists/$id"
        fun staffStatus(id: String) = "/v1/api/specialists/$id/status"
        fun staffActive(id: String) = "/v1/api/admin/staff/$id/active"
        fun staffStats(id: String) = "/v1/api/specialists/$id/stats"
        fun staffShifts(id: String) = "/v1/api/specialists/$id/shifts"
    }
}
