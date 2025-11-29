package iz.mkao.mirasalon.feature.discovery.navigation

import iz.mkao.mirasalon.core.navigation.Route
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface DiscoveryRoute : Route {
    @Serializable data object Home : DiscoveryRoute
    @Serializable data class Services(val categoryId: String? = null) : DiscoveryRoute
    @Serializable data class Products(val category: String? = null) : DiscoveryRoute
    @Serializable data class ProductDetail(val productId: String) : DiscoveryRoute
    @Serializable data class SpecialistDetail(val specialistId: String) : DiscoveryRoute
    @Serializable data object SpecialistsList : DiscoveryRoute
    @Serializable data object Locations : DiscoveryRoute

    companion object {
        val serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                subclass(Home::class)
                subclass(Services::class)
                subclass(Products::class)
                subclass(ProductDetail::class)
                subclass(SpecialistDetail::class)
                subclass(SpecialistsList::class)
                subclass(Locations::class)
            }
        }
    }
}
