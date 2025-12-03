package iz.mkao.mirasalon.feature.favourites.domain.usecase

import iz.mkao.mirasalon.core.domain.repository.FavouritesRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceFavouritesRepository
import iz.mkao.mirasalon.feature.favourites.domain.model.FavouritesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetFavouritesUseCase(
    private val favouritesRepository: FavouritesRepository,
    private val serviceFavouritesRepository: ServiceFavouritesRepository,
) {
    operator fun invoke(): Flow<FavouritesState> = combine(
        favouritesRepository.observeFavouriteProducts(),
        serviceFavouritesRepository.observeFavouriteServices(),
    ) { products, services -> FavouritesState(products, services) }
}
