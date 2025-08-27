package iz.mkao.mirasalon.core.network.mapper.admin

import io.kotest.assertions.shouldBe
import io.kotest.core.spec.style.StringSpec
import iz.mkao.mirasalon.core.network.model.dto.SalonDto

class SalonMapperTest : StringSpec({
    "should map SalonDto to domain model correctly" {
        val dto = SalonDto(
            id = "salon-123",
            name = "MiraSalon Downtown",
            address = "123 Main St",
            city = "New York",
            state = "NY",
            zipCode = "10001",
            phone = "+1234567890",
            email = "contact@mirasalon.com",
            imageUrl = "https://example.com/salon.jpg",
            rating = 4.5,
            reviewCount = 150,
            isOpen = true,
            openingHours = mapOf(
                "Monday" to "9:00-18:00",
                "Tuesday" to "9:00-18:00"
            )
        )

        val result = dto.toDomain()

        result.id shouldBe "salon-123"
        result.name shouldBe "MiraSalon Downtown"
        result.address shouldBe "123 Main St"
        result.city shouldBe "New York"
        result.state shouldBe "NY"
        result.zipCode shouldBe "10001"
        result.phone shouldBe "+1234567890"
        result.email shouldBe "contact@mirasalon.com"
        result.imageUrl shouldBe "https://example.com/salon.jpg"
        result.rating shouldBe 4.5
        result.reviewCount shouldBe 150
        result.isOpen shouldBe true
        result.openingHours.size shouldBe 2
    }
})
