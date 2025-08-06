package iz.mkao.mirasalon.feature.cart.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.OrderRepository
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.CartRoute
import kotlinx.coroutines.launch

class OrdersPresenter(
    private val screen: CartRoute.Orders,
    private val repository: OrderRepository,
    private val navigator: Navigator
) : Presenter<OrdersState> {

    @Composable
    override fun present(): OrdersState {
        var orders by remember { mutableStateOf(emptyList<OrderUiModel>()) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun fetchOrders() {
            scope.launch {
                repository.fetchOrders()
            }
        }

        LaunchedEffect(Unit) {
            fetchOrders()
            repository.observeOrders().collect { result ->
                when (result) {
                    is Outcome.Success -> {
                        orders = result.data.map { it.toUiModel() }
                        isLoading = false
                        errorMessage = null
                    }
                    is Outcome.Error -> {
                        isLoading = false
                        errorMessage = when (result.failure) {
                            is Failure.SessionExpired -> "Your session has expired. Please login again."
                            is Failure.NetworkConnection -> "No internet connection. Please check your network."
                            is Failure.ServerError -> "Server error (Code: ${(result.failure as Failure.ServerError).code}). Please try again later."
                            else -> "Failed to load orders. Please try again."
                        }
                    }
                    Outcome.Loading -> {
                        isLoading = true
                        errorMessage = null
                    }
                }
            }
        }

        return OrdersState(
            isLoading = isLoading,
            orders = orders,
            errorMessage = errorMessage,
            eventSink = { event ->
                when (event) {
                    OrdersEvent.Back -> {
                        if (screen.fromPaymentSuccess) {
                            navigator.resetRoot(BottomNavKey.Profile())
                        } else {
                            navigator.pop()
                        }
                    }
                    OrdersEvent.Retry -> fetchOrders()
                    is OrdersEvent.OrderClicked -> {
                        navigator.goTo(CartRoute.OrderDetail(event.orderId))
                    }
                    is OrdersEvent.RemoveOrder -> scope.launch {
                        repository.deleteOrder(event.orderId)
                    }
                }
            }
        )
    }
}

