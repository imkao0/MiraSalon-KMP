package iz.mkao.mirasalon.feature.favourites.domain.model

import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.Service

data class FavouritesState(
    val products: List<Product> = emptyList(),
    val services: List<Service> = emptyList(),
)