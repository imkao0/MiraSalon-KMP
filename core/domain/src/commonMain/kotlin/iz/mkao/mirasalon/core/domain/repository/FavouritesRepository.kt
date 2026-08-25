package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface FavouritesRepository {
    suspend fun isFavourite(productId: String): Boolean
    suspend fun addProductToFavourites(product: Product)
    suspend fun removeProductFromFavourites(productId: String)
    fun getFavouriteProductIds(): Flow<Set<String>>
    fun observeFavouriteProducts(): Flow<List<Product>>
}
