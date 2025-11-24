package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.domain.model.AdminAppointment
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStatus
import iz.mkao.mirasalon.core.network.model.dto.AppointmentDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import io.kotest.assertions.shouldBe
import io.kotest.core.spec.style.StringSpec
import kotlinx.datetime.Clock

class BookingMapperTest : StringSpec({
    "should map AppointmentDto to AdminAppointment correctly" {
        val dto = AppointmentDto(
            id = "apt-123",
            userId = "user-456",
            userName = "John Doe",
            specialistId = "spec-789",
            specialistName = "Jane Smith",
            salonId = "salon-001",
            salonName = "MiraSalon",
            services = listOf(
                ServiceDto(id = "svc-1", name = "Haircut", price = 50.0, durationMinutes = 30),
                ServiceDto(id = "svc-2", name = "Styling", price = 30.0, durationMinutes = 20)
            ),
            dateTime = Clock.System.now().toEpochMilliseconds(),
            durationMinutes = 50,
            totalAmount = 80.0,
            status = io.ktor.util.date.GMTDate()
        )

        val result = dto.toDomain()

        result.id shouldBe "apt-123"
        result.customerId shouldBe "user-456"
        result.customerName shouldBe "John Doe"
        result.specialistId shouldBe "spec-789"
        result.specialistName shouldBe "Jane Smith"
        result.salonId shouldBe "salon-001"
        result.salonName shouldBe "MiraSalon"
        result.serviceIds shouldBe listOf("svc-1", "svc-2")
        result.serviceNames shouldBe listOf("Haircut", "Styling")
        result.durationMinutes shouldBe 50
        result.totalAmount shouldBe 80.0
    }

    "should handle null userName gracefully" {
        val dto = AppointmentDto(
            id = "apt-123",
            userId = "user-456",
            userName = null,
            specialistId = "spec-789",
            specialistName = "Jane Smith",
            salonId = "salon-001",
            salonName = "MiraSalon",
            services = emptyList(),
            dateTime = Clock.System.now().toEpochMilliseconds(),
            durationMinutes = 30,
            totalAmount = 50.0,
            status = io.ktor.util.date.GMTDate()
        )

        val result = dto.toDomain()

        result.customerName shouldBe "Unknown"
    }
})
