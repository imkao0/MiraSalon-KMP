package iz.mkao.mirasalon.core.network.mapper.admin

import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.SpecialistDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistPerformanceDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistShiftDto
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminStaffMapperTest {

    @BeforeTest
    fun setup() {
        ApiEndpoints.setBaseUrl("http://test.com")
    }

    @Test
    fun specialistDto_toAdminSpecialist_mapsCorrecty() {
        val dto = SpecialistDto(
            id = "spec_1",
            name = "John Doe",
            role = "Senior Stylist",
            imageUrl = "/uploads/john.png",
            rating = 4.8,
            isAvailable = true,
            isOnline = true,
            status = "ONLINE",
            isActive = true,
            bio = "Expert in hair styling",
            customersServed = 150,
            salonId = "salon_1"
        )

        val domain = dto.toAdminSpecialist()

        assertEquals("spec_1", domain.id)
        assertEquals("John Doe", domain.name)
        assertEquals("Senior Stylist", domain.role)
        assertEquals("http://test.com/uploads/john.png", domain.imageUrl)
        assertEquals(4.8, domain.rating)
        assertTrue(domain.isAvailable)
        assertTrue(domain.isActive)
        assertEquals("Expert in hair styling", domain.bio)
        assertEquals(150, domain.customersServed)
    }

    @Test
    fun specialistShiftDto_toDomain_mapsCorrectly() {
        val dto = SpecialistShiftDto(
            id = "shift_1",
            specialistId = "spec_1",
            dayOfWeek = 1,
            startTime = "09:00",
            endTime = "18:00",
            isWorkingDay = true
        )

        val domain = dto.toDomain()

        assertEquals("shift_1", domain.id)
        assertEquals("spec_1", domain.specialistId)
        assertEquals(1, domain.dayOfWeek)
        assertEquals("09:00", domain.startTime)
        assertEquals("18:00", domain.endTime)
        assertTrue(domain.isActive)
    }

    @Test
    fun specialistPerformanceDto_toDomain_calculatesStats() {
        val dto = SpecialistPerformanceDto(
            specialistId = "spec_1",
            name = "John Doe",
            appointmentCount = 100,
            completionRate = 80.0,
            revenue = 5000.0,
            revenueGrowth = 15.0
        )

        val domain = dto.toDomain()

        assertEquals("spec_1", domain.specialistId)
        assertEquals("John Doe", domain.specialistName)
        assertEquals(100, domain.totalAppointments)
        assertEquals(80, domain.completedAppointments)
        assertEquals(20, domain.cancelledAppointments)
        assertEquals(5000.0, domain.revenue)
        assertEquals(15.0, domain.revenueGrowth)
    }
}
