package iz.mkao.mirasalon.core.database.dao

import androidx.room.*
import iz.mkao.mirasalon.core.database.entity.ServiceCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceCategoryDao {
    @Query("SELECT * FROM service_categories")
    fun getAllCategories(): Flow<List<ServiceCategoryEntity>>

    @Upsert
    suspend fun upsertCategories(categories: List<ServiceCategoryEntity>)

    @Query("DELETE FROM service_categories")
    suspend fun deleteAllCategories()
}
