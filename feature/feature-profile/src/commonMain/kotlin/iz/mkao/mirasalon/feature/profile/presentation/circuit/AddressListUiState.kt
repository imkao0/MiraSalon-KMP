package iz.mkao.mirasalon.feature.profile.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.profile.domain.model.Address

data class AddressListState(
    val addresses: List<Address>,
    val eventSink: (AddressListEvent) -> Unit
) : CircuitUiState

sealed interface AddressListEvent : CircuitUiEvent {
    data class DeleteAddress(val id: String) : AddressListEvent
    data class SetDefault(val id: String) : AddressListEvent
    data object AddAddress : AddressListEvent
    data class EditAddress(val id: String) : AddressListEvent
    data object Back : AddressListEvent
}
