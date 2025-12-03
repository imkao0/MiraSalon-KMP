package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStats
import iz.mkao.mirasalon.core.domain.model.SalesTrend
import iz.mkao.mirasalon.core.domain.model.ActivityEvent
import iz.mkao.mirasalon.core.domain.model.SpecialistPerformance
import iz.mkao.mirasalon.core.domain.model.ServicePopularity
import iz.mkao.mirasalon.core.domain.outcome.Outcome

interface DashboardRepository {
    suspend fun getStats(days: Int): Outcome<AdminAppointmentStats>
    suspend fun getSalesTrend(days: Int): Outcome<SalesTrend>
    suspend fun getRecentActivity(): Outcome<List<ActivityEvent>>
    suspend fun getSpecialistPerformance(days: Int): Outcome<List<SpecialistPerformance>>
    suspend fun getServicePopularity(days: Int): Outcome<List<ServicePopularity>>
}
