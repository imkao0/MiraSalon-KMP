package iz.mkao.mirasalon.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface ProfileRoute : Route {
    @Serializable
    @CommonParcelize
    data object EditProfile : ProfileRoute

    @Serializable
    @CommonParcelize
    data object Addresses : ProfileRoute

    @Serializable
    @CommonParcelize
    data class AddressForm(val addressId: String? = null) : ProfileRoute

    @Serializable
    @CommonParcelize
    data object Favourites : ProfileRoute

    @Serializable
    @CommonParcelize
    data object PaymentMethods : ProfileRoute

    @Serializable
    @CommonParcelize
    data object CurrencyAndTheme : ProfileRoute
}
