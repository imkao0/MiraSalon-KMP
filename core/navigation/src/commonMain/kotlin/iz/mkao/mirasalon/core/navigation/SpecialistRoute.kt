package iz.mkao.mirasalon.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface SpecialistRoute : Route {
    @Serializable
    @CommonParcelize
    data object Specialists : SpecialistRoute

    @Serializable
    @CommonParcelize
    data class SpecialistDetail(val specialistId: String) : SpecialistRoute
}
