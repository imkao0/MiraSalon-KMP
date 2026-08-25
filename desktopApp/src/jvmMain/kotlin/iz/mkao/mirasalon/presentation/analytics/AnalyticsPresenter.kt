package iz.mkao.mirasalon.presentation.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.DashboardRepository
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.presentation.dashboard.DesktopDashboardEvent
import iz.mkao.mirasalon.presentation.dashboard.DesktopDashboardUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnalyticsPresenter(
    private val repository: DashboardRepository,
    private val realtimeGateway: RealtimeGateway,
    private val tokenProvider: SalonTokenProvider
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
                val token = tokenProvider.accessToken()
                if (token == null) {
                    isLoading = false
                    return@launch
                }

                isLoading = true
                try {
                    coroutineScope {
                        val stats = async { repository.getOverviewStats(selectedDays) }
                        val trend = async { repository.getSalesTrend(selectedDays) }
                        val activity = async { repository.getRecentActivity() }
                        val performance = async { repository.getSpecialistPerformance(selectedDays) }
                        val popularity = async { repository.getServicePopularity(selectedDays) }

                        val statsResult = stats.await()
                        val trendResult = trend.await()
                        val activityResult = activity.await()
                        val performanceResult = performance.await()
                        val popularityResult = popularity.await()

                        when (statsResult) {
                            is Outcome.Success -> appointmentStats = statsResult.data
                            is Outcome.Error -> println("Stats error: ${statsResult.failure}")
                            Outcome.Loading -> Unit
                        }
                        when (trendResult) {
                            is Outcome.Success -> salesTrend = trendResult.data
                            is Outcome.Error -> println("Trend error: ${trendResult.failure}")
                            Outcome.Loading -> Unit
                        }
                        when (activityResult) {
                            is Outcome.Success -> recentActivity = activityResult.data
                            is Outcome.Error -> println("Activity error: ${activityResult.failure}")
                            Outcome.Loading -> Unit
                        }
                        when (performanceResult) {
                            is Outcome.Success -> specialistPerformance = performanceResult.data
                            is Outcome.Error -> println("Performance error: ${performanceResult.failure}")
                            Outcome.Loading -> Unit
                        }
                        when (popularityResult) {
                            is Outcome.Success -> servicePopularity = popularityResult.data
                            is Outcome.Error -> println("Popularity error: ${popularityResult.failure}")
                            Outcome.Loading -> Unit
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    println("AnalyticsPresenter error: ${e.message}")
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
