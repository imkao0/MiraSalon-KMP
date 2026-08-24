package iz.mkao.mirasalon.presentation.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.DashboardRepository
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.presentation.dashboard.DesktopDashboardEvent
import iz.mkao.mirasalon.presentation.dashboard.DesktopDashboardUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class AnalyticsPresenter(
    private val repository: DashboardRepository,
    private val realtimeGateway: RealtimeGateway
) : Presenter<DesktopDashboardUiState> {

    @Composable
    override fun present(): DesktopDashboardUiState {
        var salesTrend by remember { mutableStateOf(DesktopDashboardUiState().salesTrend) }
        var appointmentStats by remember { mutableStateOf(DesktopDashboardUiState().appointmentStats) }
        var specialistPerformance by remember { mutableStateOf(DesktopDashboardUiState().specialistPerformance) }
        var servicePopularity by remember { mutableStateOf(DesktopDashboardUiState().servicePopularity) }
        var recentActivity by remember { mutableStateOf(DesktopDashboardUiState().recentActivity) }
        var selectedDays by remember { mutableStateOf(DesktopDashboardUiState().selectedDays) }
        var isLoading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        var loadJob by remember { mutableStateOf<Job?>(null) }

        fun loadData() {
            loadJob?.cancel()
            loadJob = scope.launch {
                isLoading = true
                try {
                    coroutineScope {
                        val stats = async { repository.getOverviewStats(selectedDays) }
                        val trend = async { repository.getSalesTrend(selectedDays) }
                        val activity = async { repository.getRecentActivity() }
                        val performance = async { repository.getSpecialistPerformance(selectedDays) }
                        val popularity = async { repository.getServicePopularity(selectedDays) }

                        (stats.await() as? Outcome.Success)?.let { appointmentStats = it.data }
                        (trend.await() as? Outcome.Success)?.let { salesTrend = it.data }
                        (activity.await() as? Outcome.Success)?.let { recentActivity = it.data }
                        (performance.await() as? Outcome.Success)?.let { specialistPerformance = it.data }
                        (popularity.await() as? Outcome.Success)?.let { servicePopularity = it.data }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
                isLoading = false
            }
        }

        LaunchedEffect(Unit) { loadData() }

        LaunchedEffect(Unit) {
            realtimeGateway.events.collectLatest { event ->
                when (event) {
                    is DomainEvent.BookingCreated,
                    is DomainEvent.BookingUpdated,
                    is DomainEvent.OrderCreated,
                    is DomainEvent.OrderUpdated,
                    is DomainEvent.InventoryUpdated,
                    is DomainEvent.SpecialistStatusChanged,
                    is DomainEvent.ReviewSubmitted,
                    is DomainEvent.ProductChanged,
                    is DomainEvent.ServiceUpdated -> {
                        loadData()
                    }
                    else -> Unit
                }
            }
        }

        return DesktopDashboardUiState(
            salesTrend = salesTrend,
            appointmentStats = appointmentStats,
            specialistPerformance = specialistPerformance,
            servicePopularity = servicePopularity,
            recentActivity = recentActivity,
            selectedDays = selectedDays,
            isLoading = isLoading
        ) { event ->
            when (event) {
                is DesktopDashboardEvent.TimeRangeChanged -> {
                    selectedDays = event.days
                    loadData()
                }
                DesktopDashboardEvent.Refresh -> loadData()
            }
        }
    }
}
