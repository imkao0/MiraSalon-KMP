package iz.mkao.mirasalon.feature.profile.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.profile.domain.model.AddressLabel

data class AddressFormState(
    val id: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val label: AddressLabel = AddressLabel.HOME,
    val phoneNumber: String = "",
    val streetAddress: String = "",
    val number: String = "",
    val city: String = "",
    val state: String = "",
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val eventSink: (AddressFormEvent) -> Unit
) : CircuitUiState {
    val isEditing: Boolean get() = id != null
    val isValid: Boolean get() = firstName.isNotBlank() && lastName.isNotBlank() && phoneNumber.isNotBlank() && streetAddress.isNotBlank() && number.isNotBlank() && city.isNotBlank() && state.isNotBlank()
}

sealed interface AddressFormEvent : CircuitUiEvent {
    data class FirstNameChanged(val value: String) : AddressFormEvent
    data class LastNameChanged(val value: String) : AddressFormEvent
    data class LabelSelected(val label: AddressLabel) : AddressFormEvent
    data class PhoneNumberChanged(val value: String) : AddressFormEvent
    data class StreetAddressChanged(val value: String) : AddressFormEvent
    data class NumberChanged(val value: String) : AddressFormEvent
    data class CityChanged(val value: String) : AddressFormEvent
    data class StateChanged(val value: String) : AddressFormEvent
    data object Save : AddressFormEvent
    data object Delete : AddressFormEvent
    data object Back : AddressFormEvent
}
