package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.Cart
import iz.mkao.mirasalon.core.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<Cart>
    suspend fun addToCart(item: CartItem)
    suspend fun updateQuantity(productId: String, quantity: Int)
    suspend fun removeFromCart(productId: String)
    suspend fun clearCart()
    suspend fun applyCoupon(code: String): Result<Unit>
    suspend fun removeCoupon()
}
