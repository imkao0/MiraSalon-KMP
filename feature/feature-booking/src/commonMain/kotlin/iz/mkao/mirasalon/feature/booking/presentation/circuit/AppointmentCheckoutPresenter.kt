package iz.mkao.mirasalon.feature.booking.presentation.circuit

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
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.domain.model.PaymentMethodType
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.repository.PaymentMethodRepository
import iz.mkao.mirasalon.core.navigation.BookingRoute
import iz.mkao.mirasalon.feature.booking.domain.usecase.BookingUseCase
import kotlinx.coroutines.launch

class AppointmentCheckoutPresenter(
    private val screen: BookingRoute.AppointmentCheckout,
    private val bookingUseCase: BookingUseCase,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val navigator: Navigator
) : Presenter<AppointmentCheckoutState> {

    @Composable
    override fun present(): AppointmentCheckoutState {
        var isLoading by remember { mutableStateOf(true) }
        var isBooking by remember { mutableStateOf(false) }
        var specialist by remember { mutableStateOf<Specialist?>(null) }
        var services by remember { mutableStateOf(emptyList<Service>()) }
        var error by remember { mutableStateOf<String?>(null) }
        
        var showAddPaymentSheet by remember { mutableStateOf(false) }
        val paymentMethods by paymentMethodRepository.observePaymentMethods().collectAsState(initial = emptyList())
        var selectedPaymentMethodId by remember { mutableStateOf<String?>(null) }

        val scope = rememberCoroutineScope()

        LaunchedEffect(paymentMethods) {
            if (selectedPaymentMethodId == null && paymentMethods.isNotEmpty()) {
                selectedPaymentMethodId = paymentMethods.find { it.isDefault }?.id ?: paymentMethods.first().id
            }
        }

        LaunchedEffect(Unit) {
            isLoading = true
            try {
                services = bookingUseCase.loadServices(screen.serviceIds)
                isLoading = false
            } catch (e: Exception) {
                error = "Failed to load booking details"
                isLoading = false
            }
        }

        val totalAmount = services.sumOf { it.discountedPrice }
        val discountedAmount = services.sumOf { it.discountedPrice }

        return AppointmentCheckoutState(
            specialist = specialist,
            services = services,
            dateTime = screen.dateTime,
            totalAmount = totalAmount,
            discountedAmount = discountedAmount,
            paymentMethods = paymentMethods,
            selectedPaymentMethodId = selectedPaymentMethodId,
            isLoading = isLoading,
            isBooking = isBooking,
            showAddPaymentSheet = showAddPaymentSheet,
            error = error,
            eventSink = { event ->
                when (event) {
                    AppointmentCheckoutEvent.Back -> navigator.pop()
                    AppointmentCheckoutEvent.AddPaymentMethod -> showAddPaymentSheet = true
                    AppointmentCheckoutEvent.DismissAddPaymentSheet -> showAddPaymentSheet = false
                    is AppointmentCheckoutEvent.SavePaymentMethod -> {
                        scope.launch {
                            val type = when (event.type) {
                                "Visa" -> PaymentMethodType.VISA
                                "Master Card" -> PaymentMethodType.MASTER_CARD
                                else -> PaymentMethodType.CARD
                            }
                            paymentMethodRepository.addPaymentMethod(
                                type = type,
                                label = event.nameOnCard.ifBlank { "${type.displayName} Card" },
                                last4Digits = event.cardNumber.takeLast(4),
                                expiryDate = event.expiry
                            )
                            showAddPaymentSheet = false
                        }
                    }
                    is AppointmentCheckoutEvent.PaymentMethodSelected -> {
                        selectedPaymentMethodId = event.id
                    }
                    AppointmentCheckoutEvent.Continue -> {
                        Napier.d("AppointmentCheckoutPresenter: Continue clicked for services ${screen.serviceIds}")
                        scope.launch {
                            if (screen.serviceIds.isEmpty()) {
                                error = "No services selected"
                                return@launch
                            }
                            isBooking = true
                            val result = bookingUseCase.book(
                                specialistId = screen.specialistId,
                                salonId = screen.salonId,
                                serviceIds = screen.serviceIds,
                                dateTime = screen.dateTime
                            )
                            isBooking = false
                            if (result.isSuccess) {
                                val booking = result.getOrThrow()
                                navigator.goTo(BookingRoute.PaymentSuccess(booking.id))
                            } else {
                                error = "Booking failed: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    }
                }
            }
        )
    }
}
