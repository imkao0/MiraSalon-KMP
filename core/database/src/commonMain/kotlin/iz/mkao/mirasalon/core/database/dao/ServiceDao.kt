package iz.mkao.mirasalon.core.database.dao

import androidx.room.*
import iz.mkao.mirasalon.core.database.entity.ServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services")
    fun getAllServices(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE categoryId = :categoryId")
    fun getServicesByCategory(categoryId: String): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE id = :serviceId")
    suspend fun getServiceById(serviceId: String): ServiceEntity?

    @Upsert
    suspend fun upsertServices(services: List<ServiceEntity>)

    @Query("DELETE FROM services")
    suspend fun deleteAllServices()
}
