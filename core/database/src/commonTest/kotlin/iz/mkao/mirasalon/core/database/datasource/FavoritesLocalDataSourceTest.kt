package iz.mkao.mirasalon.core.database.datasource

import io.mockative.Mock
import io.mockative.classOf
import io.mockative.mock
import io.mockative.verify
import iz.mkao.mirasalon.core.database.dao.ProductFavoriteDao
import iz.mkao.mirasalon.core.database.dao.ServiceFavoriteDao
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.Service
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.kotest.assertions.shouldBe
import io.kotest.core.spec.style.StringSpec

class FavoritesLocalDataSourceTest : StringSpec({
    val productFavoriteDao = mock(classOf<ProductFavoriteDao>())
    val serviceFavoriteDao = mock(classOf<ServiceFavoriteDao>())
    
    val dataSource = FavoritesLocalDataSource(productFavoriteDao, serviceFavoriteDao)
    
    "should observe favorite products" {
        runTest {
            val products = listOf(
                Product(id = "prod-1", name = "Shampoo", category = "Hair", description = "", imageUrl = "", price = 25.0, stockQuantity = 10, discountPercent = 0.0, averageRating = 4.5, reviewCount = 100, gender = "Unisex", isActive = true),
                Product(id = "prod-2", name = "Conditioner", category = "Hair", description = "", imageUrl = "", price = 30.0, stockQuantity = 15, discountPercent = 0.0, averageRating = 4.0, reviewCount = 80, gender = "Unisex", isActive = true)
            )
            
            dataSource.observeFavoriteProducts()
        }
    }
    
    "should check if product is favorite" {
        runTest {
            dataSource.isProductFavorite("prod-1")
            
            verify(productFavoriteDao).isFavorite("prod-1")
        }
    }
    
    "should add product favorite" {
        runTest {
            val product = Product(id = "prod-1", name = "Shampoo", category = "Hair", description = "", imageUrl = "", price = 25.0, stockQuantity = 10, discountPercent = 0.0, averageRating = 4.5, reviewCount = 100, gender = "Unisex", isActive = true)
            
            dataSource.addProductFavorite(product)
            
            verify(productFavoriteDao).addFavorite(any)
        }
    }
    
    "should remove product favorite" {
        runTest {
            dataSource.removeProductFavorite("prod-1")
            
            verify(productFavoriteDao).removeFavorite("prod-1")
        }
    }
    
    "should check if service is favorite" {
        runTest {
            dataSource.isServiceFavorite("svc-1")
            
            verify(serviceFavoriteDao).isFavorite("svc-1")
        }
    }
    
    "should add service favorite" {
        runTest {
            val service = Service(id = "svc-1", name = "Haircut", description = "", durationMinutes = 30, price = 50.0, categoryId = "cat-1", subCategory = "", discountPercent = 0.0, imageUrl = "")
            
            dataSource.addServiceFavorite(service)
            
            verify(serviceFavoriteDao).addFavorite(any)
        }
    }
    
    "should remove service favorite" {
        runTest {
            dataSource.removeServiceFavorite("svc-1")
            
            verify(serviceFavoriteDao).removeFavorite("svc-1")
        }
    }
})
