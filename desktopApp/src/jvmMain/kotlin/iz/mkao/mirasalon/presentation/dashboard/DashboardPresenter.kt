package iz.mkao.mirasalon.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.common.result.NetworkResult
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.toNetworkResult
import iz.mkao.mirasalon.core.domain.repository.DashboardRepository
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.data.local.TokenManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest

class DashboardPresenter(
    private val repository: DashboardRepository,
    private val tokenManager: TokenManager,
    private val realtimeGateway: RealtimeGateway
) : Presenter<DesktopDashboardUiState> {

    @Composable
    override fun present(): DesktopDashboardUiState {
        var selectedDays by remember {
            mutableStateOf(7)
        }
        var refreshTrigger by remember {
            mutableStateOf(0L)
        }

        val session by tokenManager.session.collectAsState()
        val token = session.token

        val uiState by produceState(
            initialValue = DesktopDashboardUiState(
                userName = session.name,
                userAvatar = session.avatarUrl,
                selectedDays = selectedDays,
                isLoading = true
            ),
            token,
            selectedDays,
            refreshTrigger,
            session // Re-run if session (name/avatar) changes
        ) {
            if (token == null) {
                value = value.copy(isLoading = false, userName = session.name, userAvatar = session.avatarUrl)
                return@produceState
            }

            value = value.copy(isLoading = true, error = null, userName = session.name, userAvatar = session.avatarUrl)

            try {
                coroutineScope {
                    val statsDeferred = async { repository.getStats(selectedDays).toNetworkResult() }
                    val salesDeferred = async { repository.getSalesTrend(selectedDays).toNetworkResult() }
                    val activityDeferred = async { repository.getRecentActivity().toNetworkResult() }
                    val perfDeferred = async { repository.getSpecialistPerformance(selectedDays).toNetworkResult() }
                    val popDeferred = async { repository.getServicePopularity(selectedDays).toNetworkResult() }

                    val statsResult = statsDeferred.await()
                    val salesResult = salesDeferred.await()
                    val activityResult = activityDeferred.await()
                    val perfResult = perfDeferred.await()
                    val popResult = popDeferred.await()

                    var newStats = value.appointmentStats
                    var newSales = value.salesTrend
                    var newActivity = value.recentActivity
                    var newPerf = value.specialistPerformance
                    var newPop = value.servicePopularity
                    val errorMessage: String? = null

                    if (statsResult is NetworkResult.Success) newStats = statsResult.data
                    if (salesResult is NetworkResult.Success) newSales = salesResult.data
                    if (activityResult is NetworkResult.Success) newActivity = activityResult.data
                    if (perfResult is NetworkResult.Success) newPerf = perfResult.data
                    if (popResult is NetworkResult.Success) newPop = popResult.data

                    value = value.copy(
                        appointmentStats = newStats,
                        salesTrend = newSales,
                        recentActivity = newActivity,
                        specialistPerformance = newPerf,
                        servicePopularity = newPop,
                        error = errorMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                value = value.copy(
                    error = "System error: ${e.message}",
                    isLoading = false
                )
            }
        }

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
                        refreshTrigger = System.currentTimeMillis()
                    }

                    else -> Unit
                }
            }
        }

        return uiState.copy(
            eventSink = { event ->
                when (event) {
                    is DesktopDashboardEvent.TimeRangeChanged -> {
                        selectedDays = event.days
                    }

                    DesktopDashboardEvent.Refresh -> {
                        refreshTrigger = System.currentTimeMillis()
                    }
                }
            }
        )
    }
}
