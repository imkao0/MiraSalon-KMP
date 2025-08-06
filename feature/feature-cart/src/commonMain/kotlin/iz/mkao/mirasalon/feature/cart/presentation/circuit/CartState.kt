package iz.mkao.mirasalon.feature.cart.presentation.circuit

import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Cart
import iz.mkao.mirasalon.core.domain.model.CartItem
import iz.mkao.mirasalon.core.domain.model.Order

data class CartState(
    val cart: Cart = Cart(),
    val expiredCartItems: List<CartItem> = emptyList(),
    val expiredOrders: List<Order> = emptyList(),
    val selectedItemIds: Set<String> = emptySet(),
    val promoCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventSink: (CartEvent) -> Unit = {}
) : CircuitUiState
