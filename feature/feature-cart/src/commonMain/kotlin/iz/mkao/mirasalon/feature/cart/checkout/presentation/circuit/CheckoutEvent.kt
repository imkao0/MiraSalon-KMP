package iz.mkao.mirasalon.feature.cart.checkout.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import iz.mkao.mirasalon.core.domain.model.Address

sealed interface CheckoutEvent : CircuitUiEvent {
    data object Back : CheckoutEvent
    data class AddressSelected(val address: Address) : CheckoutEvent
    data class PaymentMethodSelected(val method: String) : CheckoutEvent
    data class PaymentMethodIdSelected(val id: String) : CheckoutEvent
    data object ProceedToPayment : CheckoutEvent
    data object ProceedToReview : CheckoutEvent
    data object PlaceOrder : CheckoutEvent
    

    data class StreetChanged(val value: String) : CheckoutEvent
    data class CityChanged(val value: String) : CheckoutEvent
    data class StateChanged(val value: String) : CheckoutEvent
    data class ZipCodeChanged(val value: String) : CheckoutEvent
    data class CountryChanged(val value: String) : CheckoutEvent
    data object SaveAddress : CheckoutEvent
    data object ToggleAddressForm : CheckoutEvent
    

    data class NameOnCardChanged(val value: String) : CheckoutEvent
    data class CardNumberChanged(val value: String) : CheckoutEvent
    data class ExpiryDateChanged(val value: String) : CheckoutEvent
    data class SecurityCodeChanged(val value: String) : CheckoutEvent
    data object BillingSameAsShippingToggled : CheckoutEvent
    data object ToggleAddCardSheet : CheckoutEvent
    data object ClearCardDetails : CheckoutEvent
}
