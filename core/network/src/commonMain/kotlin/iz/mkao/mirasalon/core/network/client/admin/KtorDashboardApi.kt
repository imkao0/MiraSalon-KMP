package iz.mkao.mirasalon.core.network.client.admin

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.ActivityEventDto
import iz.mkao.mirasalon.core.network.model.dto.AppointmentStatsDto
import iz.mkao.mirasalon.core.network.model.dto.ServicePopularityDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistPerformanceDto
import iz.mkao.mirasalon.core.network.model.dto.SalesTrendDto
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorDashboardApi(private val httpClient: HttpClient) : DashboardApi {

    override suspend fun fetchAppointmentStats(days: Int): Outcome<AppointmentStatsDto> = apiCall {
        httpClient.get(Endpoints.APPOINTMENTS) {
            parameter("days", days)
        }
    }

    override suspend fun fetchSalesTrend(days: Int): Outcome<SalesTrendDto> = apiCall {
        httpClient.get(Endpoints.SALES) {
            parameter("days", days)
        }
    }

    override suspend fun fetchRecentActivity(): Outcome<List<ActivityEventDto>> = apiCall {
        httpClient.get(Endpoints.ACTIVITY)
    }

    override suspend fun fetchSpecialistPerformance(days: Int): Outcome<List<SpecialistPerformanceDto>> = apiCall {
        httpClient.get(Endpoints.SPECIALISTS) {
            parameter("days", days)
        }
    }

    override suspend fun fetchServicePopularity(days: Int): Outcome<List<ServicePopularityDto>> = apiCall {
        httpClient.get(Endpoints.SERVICES) {
            parameter("days", days)
        }
    }

    private object Endpoints {
        const val BASE = "/v1/api/analytics"
        const val APPOINTMENTS = "$BASE/appointments"
        const val SALES = "$BASE/sales"
        const val ACTIVITY = "$BASE/activity"
        const val SPECIALISTS = "$BASE/specialists"
        const val SERVICES = "$BASE/services"
    }
}
