package iz.mkao.mirasalon.presentation.orders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.common.result.NetworkResult
import iz.mkao.mirasalon.core.domain.model.AdminOrderStatus
import iz.mkao.mirasalon.core.domain.outcome.toNetworkResult
import iz.mkao.mirasalon.core.domain.repository.AdminOrderRepository
import iz.mkao.mirasalon.presentation.components.DateFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class OrdersPresenter(
    private val repository: AdminOrderRepository
) : Presenter<OrdersUiState> {

    @Composable
    override fun present(): OrdersUiState {
        var orders by remember { mutableStateOf(OrdersUiState().orders) }
        var selectedStatus by remember { mutableStateOf(OrdersUiState().selectedStatus) }
        var searchQuery by remember { mutableStateOf(OrdersUiState().searchQuery) }
        var dateFilter by remember { mutableStateOf(OrdersUiState().dateFilter) }
        var isLoading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        var loadJob by remember { mutableStateOf<Job?>(null) }
        var actionJob by remember { mutableStateOf<Job?>(null) }

        fun loadOrders(status: AdminOrderStatus?, query: String, dateFilter: DateFilter, showLoading: Boolean = true) {
            loadJob?.cancel()
            loadJob = scope.launch {
                if (showLoading) isLoading = true

                val now = Clock.System.now()
                val tz = TimeZone.currentSystemDefault()
                val dateFrom = when (dateFilter) {
                    DateFilter.TODAY -> {
                        val localNow = now.toLocalDateTime(tz)
                        kotlinx.datetime.LocalDateTime(localNow.year, localNow.month, localNow.dayOfMonth, 0, 0, 0, 0)
                            .toInstant(tz)
                            .toEpochMilliseconds()
                    }
                    DateFilter.SEVEN_DAYS -> now.minus(7, kotlinx.datetime.DateTimeUnit.DAY, tz).toEpochMilliseconds()
                    DateFilter.THIRTY_DAYS -> now.minus(30, kotlinx.datetime.DateTimeUnit.DAY, tz).toEpochMilliseconds()
                    DateFilter.ALL -> null
                }
                val dateTo = if (dateFrom != null) now.toEpochMilliseconds() else null

                val result = try {
                    repository.getAll(
                        status = status,
                        query = query.ifBlank { null },
                        dateFrom = dateFrom,
                        dateTo = dateTo
                    ).toNetworkResult()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    NetworkResult.Error(e)
                }
                if (result is NetworkResult.Success) orders = result.data
                isLoading = false
            }
        }

        LaunchedEffect(Unit) { loadOrders(selectedStatus, searchQuery, dateFilter) }

        return OrdersUiState(
            orders = orders,
            isLoading = isLoading,
            selectedStatus = selectedStatus,
            searchQuery = searchQuery,
            dateFilter = dateFilter
        ) { event ->
            when (event) {
                is OrdersEvent.Search -> {
                    searchQuery = event.query
                    loadOrders(selectedStatus, event.query, dateFilter)
                }
                is OrdersEvent.DateFilterChanged -> {
                    dateFilter = event.filter
                    loadOrders(selectedStatus, searchQuery, event.filter)
                }
                is OrdersEvent.StatusFilterChanged -> {
                    selectedStatus = event.status
                    loadOrders(event.status, searchQuery, dateFilter)
                }
                is OrdersEvent.UpdateOrderStatus -> {
                    orders = orders.map { if (it.id == event.id) it.copy(status = event.status) else it }
                    actionJob?.cancel()
                    actionJob = scope.launch {
                        try {
                            when (repository.updateStatus(event.id, event.status).toNetworkResult()) {
                                is NetworkResult.Success -> {
                                    loadOrders(selectedStatus, searchQuery, dateFilter, showLoading = false)
                                }
                                else -> {
                                    loadOrders(selectedStatus, searchQuery, dateFilter, showLoading = false)
                                }
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            loadOrders(selectedStatus, searchQuery, dateFilter, showLoading = false)
                        }
                    }
                }
                is OrdersEvent.DeleteOrder -> {

                    orders = orders.filter { it.id != event.id }
                    
                    actionJob?.cancel()
                    actionJob = scope.launch {
                        try {
                            when (repository.delete(event.id).toNetworkResult()) {
                                is NetworkResult.Success -> loadOrders(selectedStatus, searchQuery, dateFilter, showLoading = false)
                                else -> loadOrders(selectedStatus, searchQuery, dateFilter, showLoading = false)
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            loadOrders(selectedStatus, searchQuery, dateFilter, showLoading = false)
                        }
                    }
                }
                OrdersEvent.Refresh -> loadOrders(selectedStatus, searchQuery, dateFilter)
            }
        }
    }
}
