package iz.mkao.mirasalon.feature.favourites.salon.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.Service

data class FavouritesState(
    val products: List<Product> = emptyList(),
    val services: List<Service> = emptyList(),
    val eventSink: (FavouritesEvent) -> Unit
) : CircuitUiState

sealed interface FavouritesEvent : CircuitUiEvent {
    data class ProductClicked(val id: String) : FavouritesEvent
    data class ServiceClicked(val id: String) : FavouritesEvent
    data class RemoveProductFavorite(val id: String) : FavouritesEvent
    data class RemoveServiceFavorite(val id: String) : FavouritesEvent
    data object BackClicked : FavouritesEvent
}
