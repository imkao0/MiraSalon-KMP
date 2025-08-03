package iz.mkao.mirasalon.feature.cart.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent

sealed interface CartEvent : CircuitUiEvent {
    data object Back : CartEvent
    data class UpdateQuantity(val productId: String, val quantity: Int) : CartEvent
    data class RemoveItem(val productId: String) : CartEvent
    data object Checkout : CartEvent
    data object ClearCart : CartEvent
    data class ApplyCoupon(val code: String) : CartEvent
    data object RemoveCoupon : CartEvent
    data class PromoCodeChanged(val code: String) : CartEvent
    data class ToggleSelection(val productId: String) : CartEvent
    data class ToggleStoreSelection(val storeName: String, val isSelected: Boolean) : CartEvent
    data class RemoveExpiredOrder(val orderId: String) : CartEvent
}
