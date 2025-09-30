package iz.mkao.mirasalon.presentation.customers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.result.NetworkResult
import iz.mkao.mirasalon.core.domain.outcome.toNetworkResult
import iz.mkao.mirasalon.core.domain.repository.AdminOrderRepository
import iz.mkao.mirasalon.core.domain.repository.CustomerRepository
import iz.mkao.mirasalon.core.domain.repository.DashboardRepository
import iz.mkao.mirasalon.core.domain.repository.ReviewsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class CustomersPresenter(
    private val repository: CustomerRepository,
    private val dashboardRepository: DashboardRepository,
    private val reviewsRepository: ReviewsRepository,
    private val adminOrderRepository: AdminOrderRepository
) : Presenter<CustomersUiState> {

    @Composable
    override fun present(): CustomersUiState {
        var searchQuery by remember {
            mutableStateOf("")
        }
        var selectedCustomerId by remember {
            mutableStateOf<String?>(null)
        }
        var showReviewsId by remember {
            mutableStateOf<String?>(null)
        }
        var showSalesId by remember {
            mutableStateOf<String?>(null)
        }
        var refreshTrigger by remember {
            mutableStateOf(0L)
        }

        val scope = rememberCoroutineScope()

        val uiState by produceState(
            initialValue = CustomersUiState(
                searchQuery = searchQuery,
                isLoading = true
            ),
            searchQuery,
            selectedCustomerId,
            showReviewsId,
            showSalesId,
            refreshTrigger
        ) {
            Napier.d { "CustomersPresenter produceState start - query: '$searchQuery'" }
            value = value.copy(isLoading = true, error = null)

            try {
                coroutineScope {
                    Napier.d { "CustomersPresenter fetching customers..." }
                    val customersDeferred = async {
                        repository.getAll(query = searchQuery.ifBlank { null }).toNetworkResult()
                    }
                    val statsDeferred = async {
                        dashboardRepository.getStats(days = 7).toNetworkResult()
                    }
                    val detailDeferred = if (selectedCustomerId != null) {
                        Napier.d { "CustomersPresenter fetching detail for $selectedCustomerId" }
                        async { repository.getDetail(selectedCustomerId!!).toNetworkResult() }
                    } else {
                        null
                    }

                    val reviewsDeferred = if (showReviewsId != null) {
                        async { reviewsRepository.getAll().toNetworkResult() }
                    } else {
                        null
                    }
                    
                    val salesDeferred = if (showSalesId != null) {
                        async { adminOrderRepository.getAll().toNetworkResult() }
                    } else {
                        null
                    }
                    
                    val showReviewsCustomerDeferred = if (showReviewsId != null) {
                        async { repository.getDetail(showReviewsId!!).toNetworkResult() }
                    } else {
                        null
                    }
                    
                    val showSalesCustomerDeferred = if (showSalesId != null) {
                        async { repository.getDetail(showSalesId!!).toNetworkResult() }
                    } else {
                        null
                    }

                    val customersResult = customersDeferred.await()
                    val statsResult = statsDeferred.await()
                    val detailResult = detailDeferred?.await()
                    val reviewsResult = reviewsDeferred?.await()
                    val salesResult = salesDeferred?.await()
                    val showReviewsCustomerResult = showReviewsCustomerDeferred?.await()
                    val showSalesCustomerResult = showSalesCustomerDeferred?.await()

                    var newCustomers = value.customers
                    var newSelectedCustomer = value.selectedCustomer
                    var newShowReviewsCustomer = value.showReviews
                    var newCustomerReviews = value.customerReviews
                    var newShowSalesCustomer = value.showSales
                    var newCustomerSales = value.customerSales
                    var totalCustomersCount = value.totalCustomers
                    var newCustomersCount = value.newCustomers
                    var returnRate = value.returnCustomerRate
                    var activityPoints = value.activityPoints
                    var errorMessage: String? = null

                    if (customersResult is NetworkResult.Success) {
                        newCustomers = customersResult.data
                        totalCustomersCount = newCustomers.size // Fallback if stats fail
                    }

                    if (statsResult is NetworkResult.Success) {
                        val stats = statsResult.data
                        // For 'Total', let's use confirmed + cancelled as a proxy for activity
                        // or better, if the customer list is loaded, use its size.
                        totalCustomersCount = stats.total
                        newCustomersCount = stats.points.sumOf { it.newClients }
                        val returning = stats.points.sumOf { it.returningClients }
                        val totalServed = newCustomersCount + returning
                        returnRate = if (totalServed > 0) (returning.toDouble() / totalServed) * 100.0 else 0.0
                        activityPoints = stats.points
                    }

                    if (detailResult is NetworkResult.Success) {
                        newSelectedCustomer = detailResult.data
                    } else if (selectedCustomerId == null) {
                        newSelectedCustomer = null
                    }
                    
                    if (reviewsResult is NetworkResult.Success && showReviewsId != null) {
                        newCustomerReviews = reviewsResult.data.filter { it.customerId == showReviewsId }
                    }
                    
                    if (showReviewsCustomerResult is NetworkResult.Success) {
                        newShowReviewsCustomer = showReviewsCustomerResult.data
                    } else if (showReviewsId == null) {
                        newShowReviewsCustomer = null
                    }
                    
                    if (salesResult is NetworkResult.Success && showSalesId != null) {
                        newCustomerSales = salesResult.data.filter { it.customerId == showSalesId }
                    }
                    
                    if (showSalesCustomerResult is NetworkResult.Success) {
                        newShowSalesCustomer = showSalesCustomerResult.data
                    } else if (showSalesId == null) {
                        newShowSalesCustomer = null
                    }

                    value = value.copy(
                        customers = newCustomers,
                        selectedCustomer = newSelectedCustomer,
                        showReviews = newShowReviewsCustomer,
                        customerReviews = newCustomerReviews,
                        showSales = newShowSalesCustomer,
                        customerSales = newCustomerSales,
                        totalCustomers = totalCustomersCount,
                        newCustomers = newCustomersCount,
                        returnCustomerRate = returnRate,
                        activityPoints = activityPoints,
                        error = errorMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Napier.e(e) { "CustomersPresenter exception: ${e.message}" }
                value = value.copy(
                    error = "System error: ${e.message}",
                    isLoading = false
                )
            }
        }

        return uiState.copy(
            eventSink = { event ->
                when (event) {
                    is CustomersEvent.Search -> {
                        searchQuery = event.query
                    }

                    is CustomersEvent.SelectCustomer -> {
                        selectedCustomerId = event.id
                    }

                    is CustomersEvent.ShowReviews -> {
                        showReviewsId = event.id
                    }

                    is CustomersEvent.ShowSales -> {
                        showSalesId = event.id
                    }

                    is CustomersEvent.CreateCustomer -> {
                        scope.launch {
                            try {
                                when (repository.create(event.name, event.email).toNetworkResult()) {
                                    is NetworkResult.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }

                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is CustomersEvent.UpdateCustomer -> {
                        scope.launch {
                            try {
                                when (repository.update(event.id, event.name, event.email, event.avatarUrl).toNetworkResult()) {
                                    is NetworkResult.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }

                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is CustomersEvent.DeleteCustomer -> {
                        scope.launch {
                            try {
                                when (repository.delete(event.id).toNetworkResult()) {
                                    is NetworkResult.Success -> {
                                        if (selectedCustomerId == event.id) {
                                            selectedCustomerId = null
                                        }
                                        refreshTrigger = System.currentTimeMillis()
                                    }

                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is CustomersEvent.Export -> {
                        // Placeholder for export logic
                        Napier.d { "Exporting customers data..." }
                    }

                    is CustomersEvent.Filter -> {
                        // Placeholder for filter logic
                        Napier.d { "Opening filters..." }
                    }
                }
            }
        )
    }
}
