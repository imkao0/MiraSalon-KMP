package iz.mkao.mirasalon.core.database.dao

import androidx.room.*
import iz.mkao.mirasalon.core.database.entity.PromotionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromotionDao {
    @Query("SELECT * FROM promotions WHERE status = 'ACTIVE'")
    fun getActivePromotions(): Flow<List<PromotionEntity>>

    @Upsert
    suspend fun upsertPromotions(promotions: List<PromotionEntity>)

    @Query("DELETE FROM promotions")
    suspend fun deleteAllPromotions()
}
