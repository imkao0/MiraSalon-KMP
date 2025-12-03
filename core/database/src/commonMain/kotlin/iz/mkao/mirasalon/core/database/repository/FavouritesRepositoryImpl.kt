package iz.mkao.mirasalon.core.database.repository

import iz.mkao.mirasalon.core.database.datasource.FavoritesLocalDataSource
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.repository.FavouritesRepository
import kotlinx.coroutines.flow.Flow

class FavouritesRepositoryImpl(
    private val localDataSource: FavoritesLocalDataSource
) : FavouritesRepository {
    override suspend fun isFavourite(productId: String): Boolean {
        return localDataSource.isProductFavorite(productId)
    }

    override suspend fun addProductToFavourites(product: Product) {
        localDataSource.addProductFavorite(product)
    }

    override suspend fun removeProductFromFavourites(productId: String) {
        localDataSource.removeProductFavorite(productId)
    }

    override fun getFavouriteProductIds(): Flow<Set<String>> {
        return localDataSource.getFavoriteProductIds()
    }

    override fun observeFavouriteProducts(): Flow<List<Product>> {
        return localDataSource.observeFavoriteProducts()
    }
}
