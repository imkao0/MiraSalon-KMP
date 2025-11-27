package iz.mkao.mirasalon.core.network.mapper.admin

import io.kotest.assertions.shouldBe
import io.kotest.core.spec.style.StringSpec
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStats
import iz.mkao.mirasalon.core.domain.model.AppointmentDailyPoint
import iz.mkao.mirasalon.core.domain.model.SalesTrend
import iz.mkao.mirasalon.core.network.model.dto.AppointmentDailyPoint as AppointmentDailyPointDto
import iz.mkao.mirasalon.core.network.model.dto.AppointmentStatsDto
import iz.mkao.mirasalon.core.network.model.dto.SalesTrendDto
import iz.mkao.mirasalon.core.network.model.dto.SalesTrendPoint

class DashboardMapperTest : StringSpec({
    "should map AppointmentStatsDto to AdminAppointmentStats correctly" {
        val dto = AppointmentStatsDto(
            totalConfirmed = 100,
            totalCancelled = 10,
            revenue = 5000.0,
            revenueGrowth = 15.5,
            points = emptyList()
        )

        val result = dto.toDomain()

        result.total shouldBe 110
        result.confirmed shouldBe 100
        result.completed shouldBe 100
        result.cancelled shouldBe 10
        result.revenue shouldBe 5000.0
        result.revenueGrowth shouldBe 15.5
    }

    "should map AppointmentDailyPointDto to AppointmentDailyPoint correctly" {
        val dto = AppointmentDailyPointDto(
            date = "2024-01-15",
            returningClients = 20,
            newClients = 5,
            confirmed = 25,
            cancelled = 2
        )

        val result = dto.toDomain()

        result.date shouldBe "2024-01-15"
        result.returningClients shouldBe 20
        result.newClients shouldBe 5
        result.confirmed shouldBe 25
        result.cancelled shouldBe 2
    }

    "should map SalesTrendDto to SalesTrend correctly" {
        val dto = SalesTrendDto(
            points = listOf(
                SalesTrendPoint(date = "2024-01-15", sales = 1000.0, appointments = 10),
                SalesTrendPoint(date = "2024-01-16", sales = 1500.0, appointments = 15)
            ),
            revenueGrowth = 50.0
        )

        val result = dto.toDomain()

        result.points.size shouldBe 2
        result.totalRevenue shouldBe 2500.0
        result.revenueGrowth shouldBe 50.0
    }

    "should map SalesTrendPoint to SalesDataPoint correctly" {
        val dto = SalesTrendPoint(
            date = "2024-01-15",
            sales = 1000.0,
            appointments = 10
        )

        val result = dto.toDomain()

        result.date shouldBe "2024-01-15"
        result.amount shouldBe 1000.0
        result.appointments shouldBe 10
    }
})
