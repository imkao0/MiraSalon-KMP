package iz.mkao.mirasalon.core.network.client.admin

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.*

interface DashboardApi {
    suspend fun fetchAppointmentStats(days: Int): Outcome<AppointmentStatsDto>
    suspend fun fetchOverviewStats(days: Int): Outcome<AppointmentStatsDto>
    suspend fun fetchSalesTrend(days: Int): Outcome<SalesTrendDto>
    suspend fun fetchRecentActivity(): Outcome<List<ActivityEventDto>>
    suspend fun fetchSpecialistPerformance(days: Int): Outcome<List<SpecialistPerformanceDto>>
    suspend fun fetchServicePopularity(days: Int): Outcome<List<ServicePopularityDto>>
    suspend fun fetchLowStockProducts(threshold: Int): Outcome<List<ProductDto>>
}
