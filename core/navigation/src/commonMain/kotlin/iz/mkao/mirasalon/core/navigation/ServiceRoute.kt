package iz.mkao.mirasalon.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface ServiceRoute : Route {
    @Serializable
    @CommonParcelize
    data class Services(val categoryId: String? = null) : ServiceRoute

    @Serializable
    @CommonParcelize
    data class ServiceDetail(val serviceId: String) : ServiceRoute
}
