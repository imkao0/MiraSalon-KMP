package iz.mkao.mirasalon.feature.booking.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.PaymentMethod
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.Specialist

data class AppointmentCheckoutState(
    val specialist: Specialist? = null,
    val services: List<Service> = emptyList(),
    val dateTime: Long = 0L,
    val totalAmount: Double = 0.0,
    val discountedAmount: Double = 0.0,
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val selectedPaymentMethodId: String? = null,
    val isLoading: Boolean = false,
    val isBooking: Boolean = false,
    val showAddPaymentSheet: Boolean = false,
    val error: String? = null,
    val eventSink: (AppointmentCheckoutEvent) -> Unit = {}
) : CircuitUiState

sealed interface AppointmentCheckoutEvent : CircuitUiEvent {
    data object Back : AppointmentCheckoutEvent
    data object Continue : AppointmentCheckoutEvent
    data object AddPaymentMethod : AppointmentCheckoutEvent
    data object DismissAddPaymentSheet : AppointmentCheckoutEvent
    data class SavePaymentMethod(
        val type: String,
        val nameOnCard: String,
        val cardNumber: String,
        val expiry: String,
        val cvc: String
    ) : AppointmentCheckoutEvent
    data class PaymentMethodSelected(val id: String) : AppointmentCheckoutEvent
}
