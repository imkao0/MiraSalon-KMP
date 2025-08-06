package iz.mkao.mirasalon.feature.profile.presentation.circuit

import androidx.compose.runtime.*
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.navigation.ProfileRoute
import iz.mkao.mirasalon.feature.profile.domain.repository.AddressRepository
import iz.mkao.mirasalon.feature.profile.presentation.circuit.AddressListEvent
import iz.mkao.mirasalon.feature.profile.presentation.circuit.AddressListState
import kotlinx.coroutines.launch

class AddressListPresenter(
    private val addressRepository: AddressRepository,
    private val navigator: Navigator
) : Presenter<AddressListState> {

    @Composable
    override fun present(): AddressListState {
        val addresses by addressRepository.observeAddresses().collectAsState(emptyList())
        val scope = rememberCoroutineScope()

        return AddressListState(
            addresses = addresses,
            eventSink = { event ->
                when (event) {
                    is AddressListEvent.DeleteAddress -> scope.launch { addressRepository.deleteAddress(event.id) }
                    is AddressListEvent.EditAddress -> navigator.goTo(ProfileRoute.AddressForm(event.id))
                    AddressListEvent.AddAddress -> navigator.goTo(ProfileRoute.AddressForm(null))
                    is AddressListEvent.SetDefault -> scope.launch { addressRepository.setDefault(event.id) }
                    AddressListEvent.Back -> navigator.pop()
                }
            }
        )
    }
}
