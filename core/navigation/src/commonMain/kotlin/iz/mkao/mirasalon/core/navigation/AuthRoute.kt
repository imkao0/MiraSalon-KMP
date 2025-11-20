package iz.mkao.mirasalon.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthRoute : Route {
    @Serializable
    @CommonParcelize
    data object Welcome : AuthRoute

    @Serializable
    @CommonParcelize
    data object Login : AuthRoute

    @Serializable
    @CommonParcelize
    data object Register : AuthRoute
}
