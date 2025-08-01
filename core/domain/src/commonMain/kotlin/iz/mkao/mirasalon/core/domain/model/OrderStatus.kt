package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

@Serializable

enum class OrderStatus {
    PENDING,
    SHIPPED,
    REFUNDED,
    DELIVERED,
    CANCELLED
}
