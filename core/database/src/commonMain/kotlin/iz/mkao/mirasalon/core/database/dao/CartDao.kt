package iz.mkao.mirasalon.core.database.dao

import androidx.room.*
import iz.mkao.mirasalon.core.database.entity.CartEntity
import iz.mkao.mirasalon.core.database.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Transaction
    @Query("SELECT * FROM cart_items")
    fun observeCartWithProducts(): Flow<List<CartWithProduct>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId")
    suspend fun getCartItem(productId: String): CartEntity?

    @Upsert
    suspend fun upsertCartItem(item: CartEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItem(productId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

data class CartWithProduct(
    @Embedded val cartItem: CartEntity,
    @Relation(
        parentColumn = "productId",
        entityColumn = "id"
    )
    val product: ProductEntity?
)
