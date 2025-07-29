package iz.mkao.mirasalon.feature.profile.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.navigation.ProfileRoute
import iz.mkao.mirasalon.feature.profile.domain.model.Address
import iz.mkao.mirasalon.feature.profile.domain.model.AddressLabel
import iz.mkao.mirasalon.feature.profile.domain.repository.AddressRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AddressFormPresenter(
    private val screen: ProfileRoute.AddressForm,
    private val addressRepository: AddressRepository,
    private val profileRepository: ProfileRepository,
    private val navigator: Navigator
) : Presenter<AddressFormState> {

    @Composable
    override fun present(): AddressFormState {
        val scope = rememberCoroutineScope()
        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }
        var label by remember { mutableStateOf(AddressLabel.HOME) }
        var phoneNumber by remember { mutableStateOf("") }
        var streetAddress by remember { mutableStateOf("") }
        var number by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("") }
        var state by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }
        var isDeleting by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var saved by remember { mutableStateOf(false) }
        var deleted by remember { mutableStateOf(false) }

        LaunchedEffect(screen.addressId) {
            if (screen.addressId != null) {
                addressRepository.observeAddresses().firstOrNull()?.find { it.id == screen.addressId }?.let { address ->
                    firstName = address.firstName
                    lastName = address.lastName
                    label = address.label
                    phoneNumber = address.phoneNumber
                    streetAddress = address.streetAddress
                    number = address.number
                    city = address.city
                    state = address.state
                }
            } else {
                // Pre-populate from profile for new addresses
                when (val outcome = profileRepository.getProfile()) {
                    is Outcome.Success -> {
                        val profile = outcome.data
                        val names = profile.fullName.split(" ", limit = 2)
                        firstName = names.getOrNull(0) ?: ""
                        lastName = names.getOrNull(1) ?: ""
                        phoneNumber = profile.phoneNumber ?: ""
                    }
                    else -> Unit
                }
            }
        }

        return AddressFormState(
            id = screen.addressId,
            firstName = firstName,
            lastName = lastName,
            label = label,
            phoneNumber = phoneNumber,
            streetAddress = streetAddress,
            number = number,
            city = city,
            state = state,
            isSaving = isSaving,
            isDeleting = isDeleting,
            error = errorMessage,
            saved = saved,
            deleted = deleted,
            eventSink = { event ->
                when (event) {
                    AddressFormEvent.Back -> navigator.pop()
                    is AddressFormEvent.FirstNameChanged -> firstName = event.value
                    is AddressFormEvent.LastNameChanged -> lastName = event.value
                    is AddressFormEvent.LabelSelected -> label = event.label
                    is AddressFormEvent.PhoneNumberChanged -> phoneNumber = event.value
                    is AddressFormEvent.StreetAddressChanged -> streetAddress = event.value
                    is AddressFormEvent.NumberChanged -> number = event.value
                    is AddressFormEvent.CityChanged -> city = event.value
                    is AddressFormEvent.StateChanged -> state = event.value
                    AddressFormEvent.Save -> {
                        scope.launch {
                            isSaving = true
                            errorMessage = null
                            val address = Address(
                                id = screen.addressId ?: "",
                                firstName = firstName,
                                lastName = lastName,
                                label = label,
                                phoneNumber = phoneNumber,
                                streetAddress = streetAddress,
                                number = number,
                                city = city,
                                state = state,
                                isDefault = false
                            )
                            val result = if (screen.addressId == null) {
                                addressRepository.addAddress(address)
                            } else {
                                addressRepository.updateAddress(address)
                            }
                            when (result) {
                                is Outcome.Success -> {
                                    saved = true
                                    navigator.pop()
                                }
                                is Outcome.Error -> {
                                    errorMessage = when (val f = result.failure) {
                                        is Failure.NetworkConnection -> f.message
                                        is Failure.ServerError -> f.message
                                        is Failure.ClientError -> f.message
                                        Failure.SessionExpired -> "Session expired"
                                        else -> "An error occurred"
                                    }
                                }
                                else -> Unit
                            }
                            isSaving = false
                        }
                    }
                    AddressFormEvent.Delete -> {
                        scope.launch {
                            isDeleting = true
                            errorMessage = null
                            when (val result = addressRepository.deleteAddress(screen.addressId!!)) {
                                is Outcome.Success -> {
                                    deleted = true
                                    navigator.pop()
                                }
                                is Outcome.Error -> {
                                    errorMessage = when (val f = result.failure) {
                                        is Failure.NetworkConnection -> f.message
                                        is Failure.ServerError -> f.message
                                        is Failure.ClientError -> f.message
                                        Failure.SessionExpired -> "Session expired"
                                        else -> "An error occurred"
                                    }
                                }
                                else -> Unit
                            }
                            isDeleting = false
                        }
                    }
                }
            }
        )
    }
}

