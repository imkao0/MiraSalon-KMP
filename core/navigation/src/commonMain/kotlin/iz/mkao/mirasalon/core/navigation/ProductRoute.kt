package iz.mkao.mirasalon.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface ProductRoute : Route {
    @Serializable
    @CommonParcelize
    data class Products(val category: String? = null) : ProductRoute

    @Serializable
    @CommonParcelize
    data object ExploreCategories : ProductRoute
    
    @Serializable
    @CommonParcelize
    data class ProductDetail(val productId: String) : ProductRoute
}
