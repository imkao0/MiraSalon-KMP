package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.Service
import kotlinx.coroutines.flow.Flow

interface ServiceFavouritesRepository {
    fun observeFavouriteServices(): Flow<List<Service>>
    suspend fun isFavourite(serviceId: String): Boolean
    suspend fun addServiceToFavourites(service: Service)
    suspend fun removeServiceFromFavourites(serviceId: String)
}