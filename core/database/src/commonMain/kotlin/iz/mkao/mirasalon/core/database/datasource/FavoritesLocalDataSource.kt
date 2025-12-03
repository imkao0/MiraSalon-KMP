package iz.mkao.mirasalon.core.database.datasource

import iz.mkao.mirasalon.core.database.dao.ProductFavoriteDao
import iz.mkao.mirasalon.core.database.dao.ServiceFavoriteDao
import iz.mkao.mirasalon.core.database.entity.ProductFavoriteEntity
import iz.mkao.mirasalon.core.database.entity.ServiceFavoriteEntity
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesLocalDataSource(
    private val productFavoriteDao: ProductFavoriteDao,
    private val serviceFavoriteDao: ServiceFavoriteDao
) {
    fun observeFavoriteProducts(): Flow<List<Product>> {
        return productFavoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun isProductFavorite(productId: String): Boolean {
        return productFavoriteDao.isFavorite(productId)
    }

    suspend fun addProductFavorite(product: Product) {
        productFavoriteDao.addFavorite(product.toEntity())
    }

    suspend fun removeProductFavorite(productId: String) {
        productFavoriteDao.removeFavorite(productId)
    }

    fun getFavoriteProductIds(): Flow<Set<String>> {
        return productFavoriteDao.getFavoriteIds().map { it.toSet() }
    }

    fun observeFavoriteServices(): Flow<List<Service>> {
        return serviceFavoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun isServiceFavorite(serviceId: String): Boolean {
        return serviceFavoriteDao.isFavorite(serviceId)
    }

    suspend fun addServiceFavorite(service: Service) {
        serviceFavoriteDao.addFavorite(service.toEntity())
    }

    suspend fun removeServiceFavorite(serviceId: String) {
        serviceFavoriteDao.removeFavorite(serviceId)
    }
}

private fun ProductFavoriteEntity.toDomainModel(): Product {
    return Product(
        id = productId,
        name = name,
        category = category,
        description = description,
        imageUrl = imageUrl,
        price = price,
        stockQuantity = stockQuantity,
        discountPercent = discountPercent,
        averageRating = averageRating,
        reviewCount = reviewCount,
        gender = gender,
        isActive = isActive
    )
}

private fun Product.toEntity(): ProductFavoriteEntity {
    return ProductFavoriteEntity(
        productId = id,
        name = name,
        category = category,
        description = description,
        imageUrl = imageUrl,
        price = price,
        stockQuantity = stockQuantity,
        discountPercent = discountPercent,
        averageRating = averageRating,
        reviewCount = reviewCount,
        gender = gender,
        isActive = isActive
    )
}

private fun ServiceFavoriteEntity.toDomainModel(): Service {
    return Service(
        id = serviceId,
        name = name,
        description = description,
        durationMinutes = durationMinutes,
        price = price,
        categoryId = categoryId,
        subCategory = subCategory,
        discountPercent = discountPercent,
        imageUrl = imageUrl
    )
}

private fun Service.toEntity(): ServiceFavoriteEntity {
    return ServiceFavoriteEntity(
        serviceId = id,
        name = name,
        description = description,
        durationMinutes = durationMinutes,
        price = price,
        categoryId = categoryId,
        subCategory = subCategory,
        discountPercent = discountPercent,
        imageUrl = imageUrl
    )
}
