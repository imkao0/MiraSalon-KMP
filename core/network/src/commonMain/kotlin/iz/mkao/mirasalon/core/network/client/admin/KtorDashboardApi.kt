package iz.mkao.mirasalon.core.network.client.admin

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.ActivityEventDto
import iz.mkao.mirasalon.core.network.model.dto.AppointmentStatsDto
import iz.mkao.mirasalon.core.network.model.dto.ProductDto
import iz.mkao.mirasalon.core.network.model.dto.ServicePopularityDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistPerformanceDto
import iz.mkao.mirasalon.core.network.model.dto.SalesTrendDto
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorDashboardApi(private val httpClient: HttpClient) : DashboardApi {

    override suspend fun fetchAppointmentStats(days: Int): Outcome<AppointmentStatsDto> = apiCall<AppointmentStatsDto> {
        httpClient.get(Endpoints.APPOINTMENTS) {
            parameter("days", days)
        }
    }

    override suspend fun fetchOverviewStats(days: Int): Outcome<AppointmentStatsDto> = apiCall<AppointmentStatsDto> {
        httpClient.get(Endpoints.OVERVIEW) {
            parameter("days", days)
        }
    }

    override suspend fun fetchSalesTrend(days: Int): Outcome<SalesTrendDto> = apiCall<SalesTrendDto> {
        httpClient.get(Endpoints.SALES) {
            parameter("days", days)
        }
    }

    override suspend fun fetchRecentActivity(): Outcome<List<ActivityEventDto>> = apiCall<List<ActivityEventDto>> {
        httpClient.get(Endpoints.ACTIVITY)
    }

    override suspend fun fetchSpecialistPerformance(days: Int): Outcome<List<SpecialistPerformanceDto>> = apiCall<List<SpecialistPerformanceDto>> {
        httpClient.get(Endpoints.SPECIALISTS) {
            parameter("days", days)
        }
    }

    override suspend fun fetchServicePopularity(days: Int): Outcome<List<ServicePopularityDto>> = apiCall<List<ServicePopularityDto>> {
        httpClient.get(Endpoints.SERVICES) {
            parameter("days", days)
        }
    }

    override suspend fun fetchLowStockProducts(threshold: Int): Outcome<List<ProductDto>> = apiCall<List<ProductDto>> {
        httpClient.get(Endpoints.LOW_STOCK) {
            parameter("threshold", threshold)
        }
    }

    private object Endpoints {
        const val BASE = "/v1/api/analytics"
        const val APPOINTMENTS = "$BASE/appointments"
        const val OVERVIEW = "$BASE/overview"
        const val SALES = "$BASE/sales"
        const val ACTIVITY = "$BASE/activity"
        const val SPECIALISTS = "$BASE/specialists"
        const val SERVICES = "$BASE/services"
        const val LOW_STOCK = "$BASE/low-stock"
    }
}
