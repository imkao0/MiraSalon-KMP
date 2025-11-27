package iz.mkao.mirasalon.core.network.mapper.admin

import io.kotest.assertions.shouldBe
import io.kotest.core.spec.style.StringSpec
import iz.mkao.mirasalon.core.network.mapper.admin.toDomain
import iz.mkao.mirasalon.core.network.model.dto.CustomerDetailDto
import iz.mkao.mirasalon.core.network.model.dto.CustomerSummaryDto

class CustomerMapperTest : StringSpec({
    "should map CustomerSummaryDto to CustomerSummary correctly" {
        val dto = CustomerSummaryDto(
            id = "cust-123",
            name = "John Doe",
            email = "john@example.com",
            phone = "+1234567890",
            avatarUrl = "https://example.com/avatar.jpg"
        )

        val result = dto.toDomain()

        result.id shouldBe "cust-123"
        result.name shouldBe "John Doe"
        result.email shouldBe "john@example.com"
        result.phone shouldBe "+1234567890"
        result.avatarUrl shouldBe "https://example.com/avatar.jpg"
        result.lastVisit shouldBe null
    }

    "should map CustomerDetailDto to CustomerDetail correctly" {
        val dto = CustomerDetailDto(
            id = "cust-123",
            name = "John Doe",
            email = "john@example.com",
            phone = "+1234567890",
            avatarUrl = "https://example.com/avatar.jpg",
            address = "123 Main St",
            totalSpent = 5000.0,
            visitCount = 10,
            lastVisit = "2024-01-15"
        )

        val result = dto.toDomain()

        result.id shouldBe "cust-123"
        result.name shouldBe "John Doe"
        result.email shouldBe "john@example.com"
        result.phone shouldBe "+1234567890"
        result.avatarUrl shouldBe "https://example.com/avatar.jpg"
        result.address shouldBe "123 Main St"
        result.totalSpent shouldBe 5000.0
        result.visitCount shouldBe 10
        result.lastVisit shouldBe "2024-01-15"
    }
})
