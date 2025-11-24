package iz.mkao.mirasalon.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppointmentRoute : Route {
    @Serializable
    @CommonParcelize
    data object Appointments : AppointmentRoute

    @Serializable
    @CommonParcelize
    data class AppointmentDetail(val appointmentId: String) : AppointmentRoute
}
