package iz.mkao.mirasalon.core.database.datasource

import io.mockative.Mock
import io.mockative.classOf
import io.mockative.mock
import io.mockative.verify
import iz.mkao.mirasalon.core.database.MiraDatabase
import iz.mkao.mirasalon.core.database.dao.ProductDao
import iz.mkao.mirasalon.core.domain.model.Product
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.kotest.assertions.shouldBe
import io.kotest.core.spec.style.StringSpec

class ProductLocalDataSourceTest : StringSpec({
    val database = mock(classOf<MiraDatabase>())
    val productDao = mock(classOf<ProductDao>())
    
    val dataSource = ProductLocalDataSource(database)
    
    "should observe all products" {
        runTest {
            dataSource.observeAllProducts()
        }
    }
    
    "should observe products by category" {
        runTest {
            dataSource.observeProductsByCategory("cat-1")
        }
    }
    
    "should get product by id" {
        runTest {
            dataSource.getProductById("prod-1")
        }
    }
    
    "should save products" {
        runTest {
            val products = listOf(
                Product(id = "prod-1", name = "Shampoo", category = "Hair", description = "", imageUrl = "", price = 25.0, stockQuantity = 10, discountPercent = 0.0, averageRating = 4.5, reviewCount = 100, gender = "Unisex", isActive = true)
            )
            
            dataSource.saveProducts(products)
        }
    }
    
    "should update product stock" {
        runTest {
            dataSource.updateProductStock("prod-1", 20)
        }
    }
    
    "should clear all products" {
        runTest {
            dataSource.clearAll()
        }
    }
})
