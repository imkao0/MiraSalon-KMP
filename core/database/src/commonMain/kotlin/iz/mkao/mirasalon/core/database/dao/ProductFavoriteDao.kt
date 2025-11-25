package iz.mkao.mirasalon.core.database.dao

import androidx.room.*
import iz.mkao.mirasalon.core.database.entity.ProductFavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductFavoriteDao {
    @Query("SELECT * FROM product_favorites")
    fun getAllFavorites(): Flow<List<ProductFavoriteEntity>>

    @Query("SELECT * FROM product_favorites WHERE productId = :productId")
    suspend fun getFavoriteById(productId: String): ProductFavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM product_favorites WHERE productId = :productId)")
    suspend fun isFavorite(productId: String): Boolean

    @Query("SELECT productId FROM product_favorites")
    fun getFavoriteIds(): Flow<List<String>>

    @Upsert
    suspend fun addFavorite(favorite: ProductFavoriteEntity)

    @Query("DELETE FROM product_favorites WHERE productId = :productId")
    suspend fun removeFavorite(productId: String)

    @Query("DELETE FROM product_favorites")
    suspend fun removeAllFavorites()
}
