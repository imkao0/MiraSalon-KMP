package iz.mkao.mirasalon.core.database.dao

import androidx.room.*
import iz.mkao.mirasalon.core.database.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :categoryId")
    fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: String): ProductEntity?

    @Upsert
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Query("UPDATE products SET stockQuantity = :stock WHERE id = :productId")
    suspend fun updateStock(productId: String, stock: Int)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
