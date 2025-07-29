package iz.mkao.mirasalon.feature.profile.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.PaymentMethod
import iz.mkao.mirasalon.core.domain.model.PaymentMethodType

data class PaymentMethodsState(
    val methods: List<PaymentMethod>,
    val eventSink: (PaymentMethodsEvent) -> Unit
) : CircuitUiState

sealed interface PaymentMethodsEvent : CircuitUiEvent {
    data class AddMethod(val type: PaymentMethodType, val label: String, val last4Digits: String? = null) : PaymentMethodsEvent
    data class RemoveMethod(val id: String) : PaymentMethodsEvent
    data class SetDefault(val id: String) : PaymentMethodsEvent
    data object Back : PaymentMethodsEvent
}
