package iz.mkao.mirasalon.core.database.datasource

import iz.mkao.mirasalon.core.database.MiraDatabase
import iz.mkao.mirasalon.core.database.entity.ProductEntity
import iz.mkao.mirasalon.core.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductLocalDataSource(private val database: MiraDatabase) {
    private val productDao = database.productDao()

    fun observeAllProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun observeProductsByCategory(categoryId: String): Flow<List<Product>> {
        return productDao.getProductsByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getProductById(productId: String): Product? {
        return productDao.getProductById(productId)?.toDomain()
    }

    suspend fun saveProducts(products: List<Product>) {
        productDao.upsertProducts(products.map { it.toEntity() })
    }

    suspend fun updateProductStock(productId: String, stock: Int) {
        productDao.updateStock(productId, stock)
    }

    suspend fun clearAll() {
        productDao.deleteAllProducts()
    }
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

private fun Product.toEntity() = ProductEntity(
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
