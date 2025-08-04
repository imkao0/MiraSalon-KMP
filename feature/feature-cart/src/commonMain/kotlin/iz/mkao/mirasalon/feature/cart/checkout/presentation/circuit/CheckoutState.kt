package iz.mkao.mirasalon.feature.cart.checkout.presentation.circuit

import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Address
import iz.mkao.mirasalon.core.domain.model.Cart
import iz.mkao.mirasalon.core.domain.model.PaymentMethod

enum class CheckoutStep(val index: Int, val label: String) {
    Shipping(1, "Shipping"),
    Payment(2, "Payment"),
    Review(3, "Review"),
}

data class CardDetails(
    val nameOnCard: String = "",
    val cardNumber: String = "",
    val expiryDate: String = "",
    val securityCode: String = "",
)

data class AddressInput(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "",
)

data class CheckoutState(
    val currentStep: CheckoutStep = CheckoutStep.Shipping,
    val cart: Cart = Cart(),
    val addresses: List<Address> = emptyList(),
    val selectedAddress: Address? = null,
    val addressInput: AddressInput = AddressInput(),
    val cardDetails: CardDetails = CardDetails(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val selectedPaymentMethodId: String? = null,
    val billingSameAsShipping: Boolean = true,
    val selectedPaymentMethod: String? = null,
    val isLoading: Boolean = false,
    val isPlacingOrder: Boolean = false,
    val error: String? = null,
    val showAddCardSheet: Boolean = false,
    val showAddressForm: Boolean = false,
    val customerName: String = "",
    val deliveryFee: Double = 9.90,
    val eventSink: (CheckoutEvent) -> Unit = {}
) : CircuitUiState
