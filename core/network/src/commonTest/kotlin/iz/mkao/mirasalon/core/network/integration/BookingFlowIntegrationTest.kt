package iz.mkao.mirasalon.core.network.integration

import iz.mkao.mirasalon.core.domain.model.Appointment
import iz.mkao.mirasalon.core.domain.model.AppointmentStatus
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.network.client.SalonNetworkClient
import iz.mkao.mirasalon.core.network.model.dto.CreateAppointmentRequest
import iz.mkao.mirasalon.core.network.result.NetworkError
import iz.mkao.mirasalon.core.network.result.NetworkResult
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test for the complete booking flow.
 * This test simulates the full user journey from selecting a service to confirming a booking.
 */
class BookingFlowIntegrationTest {

    @Test
    fun testCompleteBookingFlow() = runTest {
        // This would typically use a test server or mocked network client
        // For now, we'll structure it to show the integration test pattern
        
        val networkClient = createTestNetworkClient()
        
        // Step 1: Load available services
        val servicesResult = networkClient.getServices()
        assertTrue(servicesResult is NetworkResult.Success)
        val services = (servicesResult as NetworkResult.Success).data
        assertTrue(services.isNotEmpty())
        
        // Step 2: Select a service
        val selectedService = services.first()
        
        // Step 3: Load available specialists for the service
        val specialistsResult = networkClient.getSpecialists()
        assertTrue(specialistsResult is NetworkResult.Success)
        val specialists = (specialistsResult as NetworkResult.Success).data
        assertTrue(specialists.isNotEmpty())
        
        // Step 4: Select a specialist
        val selectedSpecialist = specialists.first()
        
        // Step 5: Create appointment
        val appointmentRequest = CreateAppointmentRequest(
            serviceId = selectedService.id,
            specialistId = selectedSpecialist.id,
            date = Clock.System.now().toString(),
            time = "10:00"
        )
        
        val appointmentResult = networkClient.createAppointment(appointmentRequest)
        assertTrue(appointmentResult is NetworkResult.Success)
        
        val appointment = (appointmentResult as NetworkResult.Success).data
        assertEquals(selectedService.id, appointment.serviceId)
        assertEquals(selectedSpecialist.id, appointment.specialistId)
        assertEquals(AppointmentStatus.Pending, appointment.status)
    }

    @Test
    fun testBookingFlowWithNetworkError() = runTest {
        val networkClient = createFailingNetworkClient()
        
        val servicesResult = networkClient.getServices()
        assertTrue(servicesResult is NetworkResult.Error)
    }

    private fun createTestNetworkClient(): SalonNetworkClient {
        // In a real integration test, this would connect to a test server
        // For now, return a mock implementation
        return object : SalonNetworkClient {
            override suspend fun getServices(): NetworkResult<List<Service>> {
                return NetworkResult.Success(
                    listOf(
                        Service(
                            id = "1",
                            name = "Haircut",
                            description = "Professional haircut",
                            price = 50.0,
                            duration = 30,
                            category = "Hair",
                            imageUrl = null
                        )
                    )
                )
            }

            override suspend fun getSpecialists(): NetworkResult<List<Specialist>> {
                return NetworkResult.Success(
                    listOf(
                        Specialist(
                            id = "1",
                            name = "John Doe",
                            specialty = "Haircut",
                            rating = 4.5,
                            imageUrl = null,
                            bio = "Expert hair stylist"
                        )
                    )
                )
            }

            override suspend fun createAppointment(request: CreateAppointmentRequest): NetworkResult<Appointment> {
                return NetworkResult.Success(
                    Appointment(
                        id = "1",
                        serviceId = request.serviceId,
                        serviceName = "Haircut",
                        specialistId = request.specialistId,
                        specialistName = "John Doe",
                        date = Clock.System.now(),
                        status = AppointmentStatus.Pending,
                        price = 50.0
                    )
                )
            }

            // Other required methods would be implemented here
            override suspend fun getAppointments(): NetworkResult<List<Appointment>> = NetworkResult.Success(emptyList())
            override suspend fun cancelAppointment(appointmentId: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
        }
    }

    private fun createFailingNetworkClient(): SalonNetworkClient {
        return object : SalonNetworkClient {
            override suspend fun getServices(): NetworkResult<List<Service>> {
                return NetworkResult.Error(NetworkError.NetworkError("Network unavailable"))
            }

            override suspend fun getSpecialists(): NetworkResult<List<Specialist>> {
                return NetworkResult.Error(NetworkError.NetworkError("Network unavailable"))
            }

            override suspend fun createAppointment(request: CreateAppointmentRequest): NetworkResult<Appointment> {
                return NetworkResult.Error(NetworkError.NetworkError("Network unavailable"))
            }

            override suspend fun getAppointments(): NetworkResult<List<Appointment>> = NetworkResult.Error(NetworkError.NetworkError("Network unavailable"))
            override suspend fun cancelAppointment(appointmentId: String): NetworkResult<Unit> = NetworkResult.Error(NetworkError.NetworkError("Network unavailable"))
        }
    }
}
