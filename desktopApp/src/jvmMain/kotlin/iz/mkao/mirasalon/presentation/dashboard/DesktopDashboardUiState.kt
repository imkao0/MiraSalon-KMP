package iz.mkao.mirasalon.presentation.dashboard

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.SalesTrend
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStats
import iz.mkao.mirasalon.core.domain.model.SpecialistPerformance
import iz.mkao.mirasalon.core.domain.model.ServicePopularity
import iz.mkao.mirasalon.core.domain.model.ActivityEvent
import iz.mkao.mirasalon.core.domain.model.Product

data class DesktopDashboardUiState(
    val userName: String? = null,
    val userAvatar: String? = null,
    val salesTrend: SalesTrend? = null,
    val appointmentStats: AdminAppointmentStats? = null,
    val specialistPerformance: List<SpecialistPerformance> = emptyList(),
    val servicePopularity: List<ServicePopularity> = emptyList(),
    val recentActivity: List<ActivityEvent> = emptyList(),
    val lowStockProducts: List<Product> = emptyList(),
    val selectedDays: Int = 7,
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventSink: (DesktopDashboardEvent) -> Unit = {}
) : CircuitUiState

/** Circuit UI events for the desktop dashboard screen. */
sealed interface DesktopDashboardEvent : CircuitUiEvent {
    data class TimeRangeChanged(val days: Int) : DesktopDashboardEvent
    data object Refresh : DesktopDashboardEvent
}
