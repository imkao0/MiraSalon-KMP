package iz.mkao.mirasalon.feature.cart.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.model.Order
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.OrderRepository
import iz.mkao.mirasalon.core.navigation.CartRoute
import iz.mkao.mirasalon.core.navigation.ProductRoute

class PaymentSuccessPresenter(
    private val screen: CartRoute.PaymentSuccess,
    private val repository: OrderRepository,
    private val navigator: Navigator
) : Presenter<CartPaymentSuccessState> {

    @Composable
    override fun present(): CartPaymentSuccessState {
        var order by remember { mutableStateOf<Order?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            isLoading = true
            val result = repository.getOrderDetails(screen.orderId)
            order = if (result is Outcome.Success) result.data else null
            isLoading = false
        }

        return CartPaymentSuccessState(
            order = order,
            isLoading = isLoading
        ) { event ->
            when (event) {
                CartPaymentSuccessEvent.Continue -> navigator.goTo(CartRoute.Orders(fromPaymentSuccess = true))
                CartPaymentSuccessEvent.Back -> navigator.goTo(ProductRoute.ExploreCategories)
            }
        }
    }
}

data class CartPaymentSuccessState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val eventSink: (CartPaymentSuccessEvent) -> Unit
) : CircuitUiState

sealed interface CartPaymentSuccessEvent : CircuitUiEvent {
    data object Continue : CartPaymentSuccessEvent
    data object Back : CartPaymentSuccessEvent
}
