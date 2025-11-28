package iz.mkao.mirasalon.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface NotificationRoute : Route {
    @Serializable
    @CommonParcelize
    data object Notifications : NotificationRoute

    companion object {
        val serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                subclass(Notifications::class)
            }
        }
    }
}
