package iz.mkao.mirasalon.core.database.datasource

import io.mockative.Mock
import io.mockative.classOf
import io.mockative.mock
import io.mockative.verify
import iz.mkao.mirasalon.core.database.dao.PromotionDao
import iz.mkao.mirasalon.core.domain.model.Promotion
import kotlinx.coroutines.test.runTest
import io.kotest.core.spec.style.StringSpec

class PromotionLocalDataSourceTest : StringSpec({
    val promotionDao = mock(classOf<PromotionDao>())
    
    val dataSource = PromotionLocalDataSource(promotionDao)
    
    "should observe active promotions" {
        runTest {
            dataSource.observeActivePromotions()
        }
    }
    
    "should get promotion by code" {
        runTest {
            dataSource.getPromotionByCode("SUMMER20")
            
            verify(promotionDao).getPromotionByCode("SUMMER20")
        }
    }
    
    "should save promotion" {
        runTest {
            val promotion = Promotion(
                id = "promo-1",
                code = "SUMMER20",
                description = "20% off",
                discountPercent = 20.0,
                validFrom = "2024-06-01",
                validUntil = "2024-08-31",
                isActive = true,
                applicableServiceIds = emptyList()
            )
            
            dataSource.savePromotion(promotion)
            
            verify(promotionDao).upsertPromotion(any)
        }
    }
    
    "should delete promotion" {
        runTest {
            dataSource.deletePromotion("promo-1")
            
            verify(promotionDao).deletePromotion("promo-1")
        }
    }
})
