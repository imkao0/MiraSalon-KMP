package iz.mkao.mirasalon.core.network.mapper.admin

import io.kotest.assertions.shouldBe
import io.kotest.core.spec.style.StringSpec
import iz.mkao.mirasalon.core.network.model.dto.PromotionDto

class PromotionMapperTest : StringSpec({
    "should map PromotionDto to domain model correctly" {
        val dto = PromotionDto(
            id = "promo-123",
            code = "SUMMER20",
            description = "20% off summer services",
            discountPercent = 20.0,
            validFrom = "2024-06-01",
            validUntil = "2024-08-31",
            isActive = true,
            applicableServiceIds = listOf("svc-1", "svc-2")
        )

        val result = dto.toDomain()

        result.id shouldBe "promo-123"
        result.code shouldBe "SUMMER20"
        result.description shouldBe "20% off summer services"
        result.discountPercent shouldBe 20.0
        result.validFrom shouldBe "2024-06-01"
        result.validUntil shouldBe "2024-08-31"
        result.isActive shouldBe true
        result.applicableServiceIds shouldBe listOf("svc-1", "svc-2")
    }
})
