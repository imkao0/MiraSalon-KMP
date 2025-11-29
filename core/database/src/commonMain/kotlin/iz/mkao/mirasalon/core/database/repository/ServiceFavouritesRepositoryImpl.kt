package iz.mkao.mirasalon.core.database.repository

import iz.mkao.mirasalon.core.database.datasource.FavoritesLocalDataSource
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.repository.ServiceFavouritesRepository
import kotlinx.coroutines.flow.Flow

class ServiceFavouritesRepositoryImpl(
    private val localDataSource: FavoritesLocalDataSource
) : ServiceFavouritesRepository {
    override fun observeFavouriteServices(): Flow<List<Service>> {
        return localDataSource.observeFavoriteServices()
    }

    override suspend fun isFavourite(serviceId: String): Boolean {
        return localDataSource.isServiceFavorite(serviceId)
    }

    override suspend fun addServiceToFavourites(service: Service) {
        localDataSource.addServiceFavorite(service)
    }

    override suspend fun removeServiceFromFavourites(serviceId: String) {
        localDataSource.removeServiceFavorite(serviceId)
    }
}
