package iz.mkao.mirasalon.feature.cart.checkout.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.model.Address
import iz.mkao.mirasalon.core.domain.model.Cart
import iz.mkao.mirasalon.core.domain.model.Order
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.CartRepository
import iz.mkao.mirasalon.core.domain.repository.OrderRepository
import iz.mkao.mirasalon.core.navigation.CartRoute
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.feature.profile.domain.repository.AddressRepository
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CheckoutPresenter(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val addressRepository: AddressRepository,
    private val tokenProvider: SalonTokenProvider,
    private val navigator: Navigator
) : Presenter<CheckoutState> {

    @Composable
    override fun present(): CheckoutState {
        val cart by cartRepository.observeCart().collectAsState(initial = Cart())
        var currentStep by remember { mutableStateOf(CheckoutStep.Shipping) }
        var selectedAddress by remember { mutableStateOf<Address?>(null) }
        var addressInput by remember { mutableStateOf(AddressInput()) }
        var customerName by remember { mutableStateOf("Guest Customer") }
        var customerEmail by remember { mutableStateOf("guest@example.com") }
        val scope = rememberCoroutineScope()
        val deliveryFee by remember { mutableStateOf(9.90) }
        var billingSameAsShipping by remember { mutableStateOf(true) }
        var selectedPaymentMethod by remember { mutableStateOf<String?>("Credit Card") }
        var showAddCardSheet by remember { mutableStateOf(false) }
        var showAddressForm by remember { mutableStateOf(false) }
        var isPlacingOrder by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        val hasOutOfStockItems = remember(cart.items) {
            cart.items.any { it.quantity > it.product.stockQuantity }
        }

        var cardDetails by remember { 
            mutableStateOf(
                CardDetails(
                    nameOnCard = customerName,
                    cardNumber = "",
                    expiryDate = "",
                    securityCode = ""
                )
            ) 
        }

        LaunchedEffect(Unit) {
            val name = tokenProvider.userName()
            if (!name.isNullOrBlank()) {
                customerName = name
                cardDetails = cardDetails.copy(nameOnCard = name)
            }
            val email = tokenProvider.savedEmail()
            if (!email.isNullOrBlank()) {
                customerEmail = email
            }
            addressRepository.refresh()
        }

        val profileAddresses by addressRepository.observeAddresses().collectAsState(initial = emptyList())
        val addresses = remember(profileAddresses, customerName) {
            profileAddresses.map {
                Address(
                    id = it.id,
                    name = customerName,
                    street = it.streetAddress,
                    city = it.city,
                    state = it.state,
                    zipCode = it.zipCode,
                    country = it.country,
                    isDefault = it.isDefault
                )
            }
        }

        LaunchedEffect(addresses) {
            if (selectedAddress == null && addresses.isNotEmpty()) {
                selectedAddress = addresses.find { it.isDefault } ?: addresses.firstOrNull()
            }
        }

        return CheckoutState(
            currentStep = currentStep,
            cart = cart,
            addresses = addresses,
            selectedAddress = selectedAddress,
            addressInput = addressInput,
            cardDetails = cardDetails,
            billingSameAsShipping = billingSameAsShipping,
            selectedPaymentMethod = selectedPaymentMethod,
            showAddCardSheet = showAddCardSheet,
            showAddressForm = showAddressForm,
            isPlacingOrder = isPlacingOrder,
            hasOutOfStockItems = hasOutOfStockItems,
            error = error,
            customerName = customerName,
            deliveryFee = deliveryFee,
            eventSink = { event ->
                when (event) {
                    CheckoutEvent.Back -> {
                        when (currentStep) {
                            CheckoutStep.Shipping -> navigator.pop()
                            CheckoutStep.Payment -> currentStep = CheckoutStep.Shipping
                            CheckoutStep.Review -> currentStep = CheckoutStep.Payment
                        }
                    }
                    is CheckoutEvent.AddressSelected -> selectedAddress = event.address
                    CheckoutEvent.ToggleAddressForm -> showAddressForm = !showAddressForm
                    is CheckoutEvent.StreetChanged -> addressInput = addressInput.copy(street = event.value)
                    is CheckoutEvent.CityChanged -> addressInput = addressInput.copy(city = event.value)
                    is CheckoutEvent.StateChanged -> addressInput = addressInput.copy(state = event.value)
                    is CheckoutEvent.ZipCodeChanged -> addressInput = addressInput.copy(zipCode = event.value)
                    is CheckoutEvent.CountryChanged -> addressInput = addressInput.copy(country = event.value)
                    CheckoutEvent.SaveAddress -> {
                        val currentTime = kotlin.random.Random.nextLong().toString()
                        val newAddress = Address(
                            id = currentTime,
                            name = customerName,
                            street = addressInput.street,
                            city = addressInput.city,
                            state = addressInput.state,
                            zipCode = addressInput.zipCode,
                            country = addressInput.country,
                            isDefault = true
                        )
                        selectedAddress = newAddress
                        addressInput = AddressInput()
                    }
                    is CheckoutEvent.PaymentMethodSelected -> selectedPaymentMethod = event.method
                    CheckoutEvent.ProceedToPayment -> {
                        if (addressInput.street.isNotBlank() && addressInput.city.isNotBlank() &&
                            addressInput.zipCode.isNotBlank() && addressInput.country.isNotBlank()
                        ) {
                            val currentTime = kotlin.random.Random.nextLong().toString()
                            val newAddress = Address(
                                id = currentTime,
                                name = customerName,
                                street = addressInput.street,
                                city = addressInput.city,
                                state = addressInput.state,
                                zipCode = addressInput.zipCode,
                                country = addressInput.country,
                                isDefault = true
                            )
                            selectedAddress = newAddress
                            addressInput = AddressInput()
                        }
                        if (selectedAddress != null) {
                            error = null
                            showAddressForm = false
                            currentStep = CheckoutStep.Payment
                        } else {
                            error = "Please select a saved address or fill in a new one"
                        }
                    }
                    CheckoutEvent.ProceedToReview -> {
                        currentStep = CheckoutStep.Review
                    }
                    CheckoutEvent.ToggleAddCardSheet -> showAddCardSheet = !showAddCardSheet
                    is CheckoutEvent.NameOnCardChanged -> cardDetails = cardDetails.copy(nameOnCard = event.value)
                    is CheckoutEvent.CardNumberChanged -> {
                        val filtered = event.value.filter { it.isDigit() }.take(16)
                        cardDetails = cardDetails.copy(cardNumber = filtered)
                    }
                    is CheckoutEvent.ExpiryDateChanged -> {
                        val digits = event.value.filter { it.isDigit() }
                        var formatted = ""
                        if (digits.isNotEmpty()) {
                            formatted = digits.take(2)
                            if (digits.length > 2) {
                                formatted += "/" + digits.substring(2).take(2)
                            } else if (digits.length == 2 && event.value.length > cardDetails.expiryDate.length) {
                                formatted += "/"
                            }
                        }
                        
                        if (formatted.length == 5) {
                            val month = formatted.substring(0, 2).toIntOrNull() ?: 0
                            val year = formatted.substring(3, 5).toIntOrNull() ?: 0
                            
                            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            val currentYearShort = now.year % 100
                            val currentMonth = now.monthNumber
                            
                            val isExpired = year < currentYearShort || (year == currentYearShort && month < currentMonth)
                            val isInvalidMonth = month !in 1..12
                            
                            error = when {
                                isInvalidMonth -> "Invalid month"
                                isExpired -> "Expired"
                                else -> null
                            }
                        } else {
                            error = null
                        }
                        cardDetails = cardDetails.copy(expiryDate = formatted)
                    }
                    is CheckoutEvent.SecurityCodeChanged -> {
                        val filtered = event.value.filter { it.isDigit() }.take(3)
                        cardDetails = cardDetails.copy(securityCode = filtered)
                    }
                    CheckoutEvent.ClearCardDetails -> cardDetails = CardDetails()
                    CheckoutEvent.BillingSameAsShippingToggled -> billingSameAsShipping = !billingSameAsShipping
                    CheckoutEvent.PlaceOrder -> {
                        if (selectedAddress != null) {
                            scope.launch {
                                isPlacingOrder = true
                                val order = Order(
                                    id = "",
                                    userId = tokenProvider.userId() ?: "",
                                    userName = customerName,
                                    userEmail = customerEmail,
                                    items = cart.items,
                                    subtotal = cart.subtotal,
                                    tax = 0.0,
                                    shippingFees = deliveryFee,
                                    discount = cart.discountAmount,
                                    total = cart.total + deliveryFee,
                                    placedAtEpochSeconds = 0L,
                                    promoCode = cart.couponCode,
                                    shippingAddress = selectedAddress?.let { addr ->
                                        listOf(addr.street, addr.city, addr.state, addr.zipCode, addr.country)
                                            .filter { it.isNotBlank() }
                                            .joinToString(", ")
                                    },
                                    paymentMethod = selectedPaymentMethod
                                )
                                val result = orderRepository.placeOrder(order)
                                when (result) {
                                    is Outcome.Success -> {
                                        cartRepository.clearCart()
                                        navigator.goTo(CartRoute.PaymentSuccess(result.data))
                                    }
                                    is Outcome.Error -> {
                                        error = when (val failure = result.failure) {
                                            is Failure.ServerError -> failure.message
                                            is Failure.ClientError -> failure.message
                                            is Failure.NetworkConnection -> failure.message
                                            is Failure.SessionExpired -> "Session expired. Please log in again."
                                            else -> "Failed to place order"
                                        }
                                    }
                                    is Outcome.Loading -> {}
                                }
                                isPlacingOrder = false
                            }
                        }
                    }
                    else -> {}
                }
            }
        )
    }
}
