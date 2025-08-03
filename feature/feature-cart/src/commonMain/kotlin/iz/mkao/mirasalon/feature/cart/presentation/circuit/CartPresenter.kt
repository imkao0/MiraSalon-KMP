package iz.mkao.mirasalon.feature.cart.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.domain.model.Cart
import iz.mkao.mirasalon.core.domain.model.OrderStatus
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.CartRepository
import iz.mkao.mirasalon.core.domain.repository.OrderRepository
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.CartRoute
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.feature.cart.checkout.presentation.circuit.CheckoutPresenter
import iz.mkao.mirasalon.feature.profile.domain.repository.AddressRepository
import kotlinx.coroutines.launch

class CartPresenter(
    private val repository: CartRepository,
    private val orderRepository: OrderRepository,
    private val navigator: Navigator
) : Presenter<CartState> {

    @Composable
    override fun present(): CartState {
        val rawCart by remember { repository.observeCart() }.collectAsState(initial = Cart())
        val ordersOutcome by remember { orderRepository.observeOrders() }.collectAsState(initial = Outcome.Loading)

        val cart = remember(rawCart) {
            rawCart.copy(items = rawCart.items.filter { it.product.isActive })
        }

        val expiredCartItems = remember(rawCart) {
            rawCart.items.filter { !it.product.isActive }
        }
        
        val expiredOrders = remember(ordersOutcome) {
            val now = kotlin.time.Clock.System.now().epochSeconds
            (ordersOutcome as? Outcome.Success)?.data?.filter { 
                it.status == OrderStatus.CANCELLED || (it.expiresAt != null && it.expiresAt!! < now)
            } ?: emptyList()
        }

        var selectedItemIds by remember { mutableStateOf(emptySet<String>()) }
        
        // Optimistically select all items on first load
        LaunchedEffect(cart.items) {
            if (selectedItemIds.isEmpty() && cart.items.isNotEmpty()) {
                selectedItemIds = cart.items.map { it.product.id }.toSet()
            }
        }

        var promoCode by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        // Sync local promoCode with cart's coupon code if it changes
        LaunchedEffect(cart.couponCode) {
            if (cart.couponCode != null && promoCode.isEmpty()) {
                promoCode = cart.couponCode!!
            }
        }

        return CartState(
            cart = cart,
            expiredCartItems = expiredCartItems,
            expiredOrders = expiredOrders,
            selectedItemIds = selectedItemIds,
            promoCode = promoCode,
            error = error,
            eventSink = { event ->
                when (event) {
                    CartEvent.Back -> navigator.pop()
                    is CartEvent.UpdateQuantity -> scope.launch {
                        repository.updateQuantity(event.productId, event.quantity)
                    }
                    is CartEvent.RemoveItem -> scope.launch {
                        repository.removeFromCart(event.productId)
                    }
                    CartEvent.Checkout -> navigator.goTo(CartRoute.Checkout)
                    CartEvent.ClearCart -> scope.launch {
                        repository.clearCart()
                    }
                    is CartEvent.ApplyCoupon -> scope.launch {
                        error = null
                        val result = repository.applyCoupon(event.code)
                        if (result.isFailure) {
                            error = result.exceptionOrNull()?.message ?: "Invalid coupon code"
                        }
                    }
                    is CartEvent.RemoveCoupon -> scope.launch {
                        repository.removeCoupon()
                        promoCode = ""
                        error = null
                    }
                    is CartEvent.PromoCodeChanged -> {
                        promoCode = event.code
                        error = null
                    }
                    is CartEvent.ToggleSelection -> {
                        selectedItemIds = if (selectedItemIds.contains(event.productId)) {
                            selectedItemIds - event.productId
                        } else {
                            selectedItemIds + event.productId
                        }
                    }
                    is CartEvent.ToggleStoreSelection -> {
                        val storeItems = cart.items.filter { it.product.providerName == event.storeName }
                            .map { it.product.id }
                        selectedItemIds = if (event.isSelected) {
                            selectedItemIds + storeItems
                        } else {
                            selectedItemIds - storeItems.toSet()
                        }
                    }
                    is CartEvent.RemoveExpiredOrder -> scope.launch {
                        orderRepository.deleteOrder(event.orderId)
                    }
                }
            }
        )
    }
}

class CartManualPresenterFactory(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val addressRepository: AddressRepository,
    private val tokenProvider: SalonTokenProvider
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is CartRoute.Cart -> CartPresenter(cartRepository, orderRepository, navigator)
            is BottomNavKey.Cart -> CartPresenter(cartRepository, orderRepository, navigator)
            is CartRoute.Checkout -> CheckoutPresenter(cartRepository, orderRepository, addressRepository, tokenProvider, navigator)
            is CartRoute.Orders -> OrdersPresenter(screen, orderRepository, navigator)
            is CartRoute.OrderDetail -> OrderDetailPresenter(screen, orderRepository, navigator)
            is CartRoute.PaymentSuccess -> PaymentSuccessPresenter(screen, orderRepository, navigator)
            else -> null
        }
    }
}
