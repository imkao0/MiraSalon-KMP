package iz.mkao.mirasalon.core.database.datasource

import iz.mkao.mirasalon.core.database.MiraDatabase
import iz.mkao.mirasalon.core.database.dao.CartWithProduct
import iz.mkao.mirasalon.core.database.entity.CartEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CartLocalDataSource(private val database: MiraDatabase) {
    private val cartDao = database.cartDao()

    fun observeCartItems(): Flow<List<CartEntity>> = cartDao.observeCartWithProducts().map { list ->
        list.map { it.cartItem }
    }

    fun observeCartWithProducts(): Flow<List<CartWithProduct>> = cartDao.observeCartWithProducts()

    suspend fun getCartItem(productId: String): CartEntity? = cartDao.getCartItem(productId)

    suspend fun saveCartItem(item: CartEntity) = cartDao.upsertCartItem(item)

    suspend fun removeCartItem(productId: String) = cartDao.deleteCartItem(productId)

    suspend fun clearCart() = cartDao.clearCart()
}
