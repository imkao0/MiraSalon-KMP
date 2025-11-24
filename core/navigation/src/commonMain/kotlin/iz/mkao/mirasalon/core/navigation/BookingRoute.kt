package iz.mkao.mirasalon.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface BookingRoute : Route {
    @Serializable
    @CommonParcelize
    data class Booking(
        val serviceIds: List<String> = emptyList(),
        val specialistId: String? = null
    ) : BookingRoute

    @Serializable
    @CommonParcelize
    data class AppointmentCheckout(
        val serviceIds: List<String>,
        val specialistId: String,
        val salonId: String,
        val dateTime: Long
    ) : BookingRoute

    @Serializable
    @CommonParcelize
    data class PaymentSuccess(val appointmentId: String) : BookingRoute

    @Serializable
    @CommonParcelize
    data class EReceipt(val appointmentId: String = "") : BookingRoute
}
