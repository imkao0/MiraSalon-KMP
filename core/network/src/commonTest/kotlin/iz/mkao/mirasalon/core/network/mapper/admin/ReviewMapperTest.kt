package iz.mkao.mirasalon.core.network.mapper.admin

import io.kotest.assertions.shouldBe
import io.kotest.core.spec.style.StringSpec
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto

class ReviewMapperTest : StringSpec({
    "should map ReviewDto to domain model correctly" {
        val dto = ReviewDto(
            id = "review-123",
            customerId = "cust-456",
            customerName = "John Doe",
            customerAvatarUrl = "https://example.com/avatar.jpg",
            specialistId = "spec-789",
            specialistName = "Jane Smith",
            rating = 5,
            comment = "Excellent service!",
            createdAt = 1704067200000L
        )

        val result = dto.toDomain()

        result.id shouldBe "review-123"
        result.customerId shouldBe "cust-456"
        result.customerName shouldBe "John Doe"
        result.customerAvatarUrl shouldBe "https://example.com/avatar.jpg"
        result.specialistId shouldBe "spec-789"
        result.specialistName shouldBe "Jane Smith"
        result.rating shouldBe 5
        result.comment shouldBe "Excellent service!"
        result.createdAt shouldBe 1704067200000L
    }
})