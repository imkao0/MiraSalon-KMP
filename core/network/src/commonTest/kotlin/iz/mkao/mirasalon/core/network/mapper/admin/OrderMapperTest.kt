package iz.mkao.mirasalon.core.network.mapper.admin

import io.kotest.assertions.shouldBe
import io.kotest.core.spec.style.StringSpec
import iz.mkao.mirasalon.core.network.model.dto.OrderDto
import iz.mkao.mirasalon.core.network.model.dto.OrderItemDto

class OrderMapperTest : StringSpec({
    "should map OrderDto to domain model correctly" {
        val dto = OrderDto(
            id = "order-123",
            customerId = "cust-456",
            customerName = "John Doe",
            customerEmail = "john@example.com",
            status = "CONFIRMED",
            totalAmount = 150.0,
            createdAt = 1704067200000L,
            items = listOf(
                OrderItemDto(
                    productId = "prod-1",
                    productName = "Shampoo",
                    quantity = 2,
                    price = 25.0
                ),
                OrderItemDto(
                    productId = "prod-2",
                    productName = "Conditioner",
                    quantity = 1,
                    price = 100.0
                )
            )
        )

        val result = dto.toDomain()

        result.id shouldBe "order-123"
        result.customerId shouldBe "cust-456"
        result.customerName shouldBe "John Doe"
        result.customerEmail shouldBe "john@example.com"
        result.status shouldBe "CONFIRMED"
        result.totalAmount shouldBe 150.0
        result.items.size shouldBe 2
        result.items[0].productId shouldBe "prod-1"
        result.items[0].productName shouldBe "Shampoo"
        result.items[0].quantity shouldBe 2
        result.items[0].price shouldBe 25.0
    }
})
