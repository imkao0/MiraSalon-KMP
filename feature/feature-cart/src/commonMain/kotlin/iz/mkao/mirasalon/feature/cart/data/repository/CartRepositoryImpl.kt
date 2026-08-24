package iz.mkao.mirasalon.feature.cart.data.repository

import iz.mkao.mirasalon.core.database.datasource.CartLocalDataSource
import iz.mkao.mirasalon.core.database.datasource.ProductLocalDataSource
import iz.mkao.mirasalon.core.database.entity.CartEntity
import iz.mkao.mirasalon.core.database.entity.ProductEntity
import iz.mkao.mirasalon.core.domain.model.Cart
import iz.mkao.mirasalon.core.domain.model.CartItem
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.CartRepository
import iz.mkao.mirasalon.core.domain.repository.PromoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class CartRepositoryImpl(
    private val localDataSource: CartLocalDataSource,
    private val productLocalDataSource: ProductLocalDataSource,
    private val promoRepository: PromoRepository,
    private val repositoryScope: CoroutineScope
) : CartRepository {

    private val _couponCode = MutableStateFlow<String?>(null)
    private val _discountAmount = MutableStateFlow(0.0)

    override fun observeCart(): Flow<Cart> {
        return combine(
            localDataSource.observeCartWithProducts(),
            _couponCode,
            _discountAmount
        ) { entities, coupon, discount ->
            val items = entities.mapNotNull { it.product?.let { entity ->
                CartItem(
                    product = entity.toDomain(),
                    quantity = it.cartItem.quantity
                )
            }}
            Cart(items = items, couponCode = coupon, discountAmount = discount)
        }
    }

    override suspend fun addToCart(item: CartItem) {
        val existing = localDataSource.getCartItem(item.product.id)
        val newQuantity = (existing?.quantity ?: 0) + item.quantity
        localDataSource.saveCartItem(CartEntity(productId = item.product.id, quantity = newQuantity))
    }

    override suspend fun updateQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            localDataSource.removeCartItem(productId)
        } else {
            localDataSource.saveCartItem(CartEntity(productId = productId, quantity = quantity))
        }
    }

    override suspend fun removeFromCart(productId: String) {
        localDataSource.removeCartItem(productId)
    }

    override suspend fun clearCart() {
        localDataSource.clearCart()
        _couponCode.value = null
        _discountAmount.value = 0.0
    }

    override suspend fun applyCoupon(code: String): Result<Unit> {
        val normalized = code.trim().uppercase()
        if (normalized.isEmpty()) {
            removeCoupon()
            return Result.success(Unit)
        }
        if (normalized.length < MIN_COUPON_LENGTH) {
            return Result.failure(IllegalArgumentException("Invalid coupon code"))
        }

        val cart = observeCart().first()
        val result = promoRepository.validatePromo(normalized, cart.items)

        return when (result) {
            is Outcome.Success -> {
                if (result.data.isValid) {
                    _couponCode.value = normalized
                    _discountAmount.value = result.data.discountAmount
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(result.data.errorMessage ?: "Invalid coupon"))
                }
            }
            is Outcome.Error -> {
                val message = when (val failure = result.failure) {
                    is Failure.ServerError -> failure.message
                    is Failure.ClientError -> failure.message
                    is Failure.NetworkConnection -> failure.message
                    is Failure.SessionExpired -> "Session expired. Please log in again."
                    else -> "Failed to validate coupon"
                }
                Result.failure(Exception(message))
            }
            is Outcome.Loading -> {
                Result.failure(Exception("Validation in progress"))
            }
        }
    }

    override suspend fun removeCoupon() {
        _couponCode.value = null
        _discountAmount.value = 0.0
    }

    private fun ProductEntity.toDomain() = Product(
        id = id,
        name = name,
        category = category,
        description = description,
        imageUrl = imageUrl,
        price = price,
        stockQuantity = stockQuantity,
        discountPercent = discountPercent,
        averageRating = averageRating,
        reviewCount = reviewCount,
        isActive = isActive
    )

    private companion object {
        const val MIN_COUPON_LENGTH = 3
    }
}
