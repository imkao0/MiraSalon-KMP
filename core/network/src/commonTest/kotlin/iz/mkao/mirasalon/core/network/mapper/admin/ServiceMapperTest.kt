package iz.mkao.mirasalon.core.network.mapper.admin

import io.kotest.assertions.shouldBe
import io.kotest.core.spec.style.StringSpec
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto

class ServiceMapperTest : StringSpec({
    "should map ServiceDto to domain model correctly" {
        val dto = ServiceDto(
            id = "svc-123",
            name = "Haircut",
            description = "Professional haircut service",
            price = 50.0,
            durationMinutes = 30,
            categoryId = "cat-1",
            categoryName = "Hair Services",
            imageUrl = "https://example.com/service.jpg",
            isActive = true
        )

        val result = dto.toDomain()

        result.id shouldBe "svc-123"
        result.name shouldBe "Haircut"
        result.description shouldBe "Professional haircut service"
        result.price shouldBe 50.0
        result.durationMinutes shouldBe 30
        result.categoryId shouldBe "cat-1"
        result.categoryName shouldBe "Hair Services"
        result.imageUrl shouldBe "https://example.com/service.jpg"
        result.isActive shouldBe true
    }
})
