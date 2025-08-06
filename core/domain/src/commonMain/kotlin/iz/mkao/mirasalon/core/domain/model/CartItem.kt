package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable

data class CartItem(
    val product: Product,
    val quantity: Int
)
