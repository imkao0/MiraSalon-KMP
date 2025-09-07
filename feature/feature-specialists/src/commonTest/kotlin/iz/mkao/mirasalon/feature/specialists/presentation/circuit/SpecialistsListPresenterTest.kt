package iz.mkao.mirasalon.feature.specialists.presentation.circuit

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.network.result.NetworkError
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.feature.specialists.data.repository.SpecialistsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpecialistsListPresenterTest {

    private val fakeRepository = FakeSpecialistsRepository()
    private val navigator = FakeNavigator()

    @Test
    fun specialistsPresenter_loadsSpecialists() = runTest {
        val presenter = SpecialistsListPresenter(fakeRepository, navigator)
        presenter.test {
            val state = awaitItem()
            
            // Initially loading
            assertTrue(state.isLoading)
            
            // Wait for data to load
            val loadedState = awaitItem()
            assertTrue(!loadedState.isLoading)
            assertEquals(2, loadedState.specialists.size)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun specialistsPresenter_filtersByService() = runTest {
        val presenter = SpecialistsListPresenter(fakeRepository, navigator)
        presenter.test {
            val state = awaitItem()
            
            // Wait for data to load
            awaitItem()
            
            state.eventSink(SpecialistsEvent.FilterByService("Haircut"))
            
            // Wait for filter to apply
            val filteredState = awaitItem()
            assertEquals(1, filteredState.specialists.size)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun specialistsPresenter_handlesError() = runTest {
        fakeRepository.shouldSucceed = false
        val presenter = SpecialistsListPresenter(fakeRepository, navigator)
        presenter.test {
            val state = awaitItem()
            
            // Wait for error state
            var currentState = awaitItem()
            while (currentState.error == null) {
                currentState = awaitItem()
            }
            
            assertTrue(currentState.error != null)
            assertTrue(!currentState.isLoading)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeSpecialistsRepository : SpecialistsRepository {
        var shouldSucceed = true

        override suspend fun getSpecialists(): NetworkResult<List<Specialist>> {
            delay(10)
            return if (shouldSucceed) {
                NetworkResult.Success(
                    listOf(
                        Specialist(
                            id = "1",
                            name = "John Doe",
                            specialty = "Haircut",
                            rating = 4.5,
                            imageUrl = null,
                            bio = "Expert hair stylist"
                        ),
                        Specialist(
                            id = "2",
                            name = "Jane Smith",
                            specialty = "Massage",
                            rating = 4.8,
                            imageUrl = null,
                            bio = "Certified massage therapist"
                        )
                    )
                )
            } else {
                NetworkResult.Error(NetworkError.HttpError(500, "Failed to load specialists"))
            }
        }

        override suspend fun getSpecialistById(id: String): NetworkResult<Specialist> {
            return if (shouldSucceed) {
                NetworkResult.Success(
                    Specialist(
                        id = id,
                        name = "John Doe",
                        specialty = "Haircut",
                        rating = 4.5,
                        imageUrl = null,
                        bio = "Expert hair stylist"
                    )
                )
            } else {
                NetworkResult.Error(NetworkError.HttpError(404, "Specialist not found"))
            }
        }
    }
}
