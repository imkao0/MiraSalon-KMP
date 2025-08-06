package iz.mkao.mirasalon.feature.profile.presentation.circuit

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.ProfileRoute
import iz.mkao.mirasalon.feature.profile.presentation.ui.ProfileScreenWrapper

class ProfileManualUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is BottomNavKey.Profile -> ui<ProfileState> { state, modifier ->
                ProfileScreenWrapper(state, modifier)
            }
            is ProfileRoute.EditProfile -> ui<EditProfileState> { state, modifier ->
                EditProfileUi(state, modifier)
            }
            is ProfileRoute.Addresses -> ui<AddressListState> { state, modifier ->
                AddressListUi(state, modifier)
            }
            is ProfileRoute.AddressForm -> ui<AddressFormState> { state, modifier ->
                AddressFormUi(state, modifier)
            }
            is ProfileRoute.PaymentMethods -> ui<PaymentMethodsState> { state, modifier ->
                PaymentMethodsUi(state, modifier)
            }
            is ProfileRoute.CurrencyAndTheme -> ui<ProfileState> { state, modifier ->
                ProfileScreenWrapper(state, modifier)
            }
            else -> null
        }
    }
}
