package iz.mkao.mirasalon.data.repository

import iz.mkao.mirasalon.core.domain.model.ActivityEvent
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStats
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.SalesTrend
import iz.mkao.mirasalon.core.domain.model.ServicePopularity
import iz.mkao.mirasalon.core.domain.model.SpecialistPerformance
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.DashboardRepository
import iz.mkao.mirasalon.core.network.client.admin.DashboardApi
import iz.mkao.mirasalon.core.network.mapper.admin.toDomain
import iz.mkao.mirasalon.core.network.mapper.admin.toPerformanceDomain
import iz.mkao.mirasalon.core.network.mapper.admin.toPopularityDomain
import iz.mkao.mirasalon.feature.products.data.mapper.toDomain

class KtorDashboardRepository(
    private val api: DashboardApi
) : DashboardRepository {

    override suspend fun getStats(days: Int): Outcome<AdminAppointmentStats> {
        return api.fetchAppointmentStats(days).map { it.toDomain() }
    }

    override suspend fun getOverviewStats(days: Int): Outcome<AdminAppointmentStats> {
        return api.fetchOverviewStats(days).map { it.toDomain() }
    }

    override suspend fun getSalesTrend(days: Int): Outcome<SalesTrend> {
        return api.fetchSalesTrend(days).map { it.toDomain() }
    }

    override suspend fun getRecentActivity(): Outcome<List<ActivityEvent>> {
        return api.fetchRecentActivity().map { dtos ->
            dtos.map { it.toDomain() }
        }
    }

    override suspend fun getSpecialistPerformance(days: Int): Outcome<List<SpecialistPerformance>> {
        return api.fetchSpecialistPerformance(days).map { dtos ->
            dtos.map { it.toPerformanceDomain() }
        }
    }

    override suspend fun getServicePopularity(days: Int): Outcome<List<ServicePopularity>> {
        return api.fetchServicePopularity(days).map { dtos ->
            dtos.map { it.toPopularityDomain() }
        }
    }

    override suspend fun getLowStockProducts(threshold: Int): Outcome<List<Product>> {
        return api.fetchLowStockProducts(threshold).map { dtos ->
            dtos.map { it.toDomain() }
        }
    }
}
