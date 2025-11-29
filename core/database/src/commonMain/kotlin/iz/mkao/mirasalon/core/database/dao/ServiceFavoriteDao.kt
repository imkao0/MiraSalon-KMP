package iz.mkao.mirasalon.core.database.dao

import androidx.room.*
import iz.mkao.mirasalon.core.database.entity.ServiceFavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceFavoriteDao {
    @Query("SELECT * FROM service_favorites")
    fun getAllFavorites(): Flow<List<ServiceFavoriteEntity>>

    @Query("SELECT * FROM service_favorites WHERE serviceId = :serviceId")
    suspend fun getFavoriteById(serviceId: String): ServiceFavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM service_favorites WHERE serviceId = :serviceId)")
    suspend fun isFavorite(serviceId: String): Boolean

    @Upsert
    suspend fun addFavorite(favorite: ServiceFavoriteEntity)

    @Query("DELETE FROM service_favorites WHERE serviceId = :serviceId")
    suspend fun removeFavorite(serviceId: String)

    @Query("DELETE FROM service_favorites")
    suspend fun removeAllFavorites()
}
