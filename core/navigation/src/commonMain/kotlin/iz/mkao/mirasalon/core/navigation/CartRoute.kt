package iz.mkao.mirasalon.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface CartRoute : Route {
    @Serializable
    @CommonParcelize
    data object Cart : CartRoute

    @Serializable
    @CommonParcelize
    data object Checkout : CartRoute

    @Serializable
    @CommonParcelize
    data class Orders(val fromPaymentSuccess: Boolean = false) : CartRoute

    @Serializable
    @CommonParcelize
    data class OrderDetail(
        val orderId: String,
        val fromCheckout: Boolean = false
    ) : CartRoute

    @Serializable
    @CommonParcelize
    data class PaymentSuccess(val orderId: String) : CartRoute
}
