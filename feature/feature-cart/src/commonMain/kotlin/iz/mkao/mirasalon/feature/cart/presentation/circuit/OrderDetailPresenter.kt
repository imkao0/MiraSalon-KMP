package iz.mkao.mirasalon.feature.cart.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.core.domain.model.Order
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.OrderRepository
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.CartRoute

class OrderDetailPresenter(
    private val screen: CartRoute.OrderDetail,
    private val repository: OrderRepository,
    private val navigator: Navigator
) : Presenter<OrderDetailState> {

    @Composable
    override fun present(): OrderDetailState {
        var order by remember { mutableStateOf<Order?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(screen.orderId) {
            val result = repository.getOrderDetails(screen.orderId)
            when (result) {
                is Outcome.Success -> {
                    order = result.data
                    isLoading = false
                }
                is Outcome.Error -> {
                    error = "Failed to load order details"
                    isLoading = false
                }
                is Outcome.Loading -> {}
            }
        }

        return OrderDetailState(
            order = order,
            placedAt = order?.let { DateUtils.formatDateTime(it.placedAtEpochSeconds) } ?: "",
            isLoading = isLoading,
            error = error,
            fromCheckout = screen.fromCheckout,
            eventSink = { event ->
                when (event) {
                    OrderDetailEvent.Back -> navigator.pop()
                    OrderDetailEvent.Home -> {
                        navigator.resetRoot(BottomNavKey.Home())
                    }
                }
            }
        )
    }
}
