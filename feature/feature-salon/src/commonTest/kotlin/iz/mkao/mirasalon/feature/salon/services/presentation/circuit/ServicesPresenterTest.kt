package iz.mkao.mirasalon.feature.salon.services.presentation.circuit

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import iz.mkao.mirasalon.core.domain.model.Review
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.domain.model.ServiceFilter
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.navigation.ServiceRoute
import iz.mkao.mirasalon.core.domain.model.CartItem
import iz.mkao.mirasalon.core.domain.model.PromoValidation
import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.repository.PromoRepository
import iz.mkao.mirasalon.feature.salon.services.data.repository.ServiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServicesPresenterTest {

    private val repository = FakeServiceRepository()
    private val promoRepository = FakePromoRepository()
    private val navigator = FakeNavigator(ServiceRoute.Services(null))

    @Test
    fun presenter_initial_state_loads_categories() = runTest {
        val screen = ServiceRoute.Services(null)
        val presenter = ServicesPresenter(screen, repository, promoRepository, navigator)

        presenter.test {
            val state = awaitItem()
            
            // Wait for categories to load
            var currentState = state
            while (currentState.categories.isEmpty()) {
                currentState = awaitItem()
            }
            
            assertEquals(1, currentState.categories.size)
            assertEquals("Cat 1", currentState.categories[0].name)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun presenter_updates_search_query() = runTest {
        val screen = ServiceRoute.Services(null)
        val presenter = ServicesPresenter(screen, repository, promoRepository, navigator)

        presenter.test {
            // Wait until initialization is complete and we have categories
            var currentState = awaitItem()
            while (currentState.categories.isEmpty() || currentState.isLoading) {
                currentState = awaitItem()
            }
            
            // Now that we are stable, trigger search
            currentState.eventSink(ServicesEvent.SearchQueryChanged("test"))
            
            // Consume states until search query is updated
            currentState = awaitItem()
            while (currentState.searchQuery != "test") {
                currentState = awaitItem()
            }
            assertEquals("test", currentState.searchQuery)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeServiceRepository : ServiceRepository {
        override suspend fun getCategories(): Outcome<List<ServiceCategory>> = 
            Outcome.Success(listOf(ServiceCategory("1", "Cat 1", "icon", null)))
        
        override suspend fun getServices(filter: ServiceFilter): Outcome<List<Service>> = 
            Outcome.Success(emptyList())
        
        override suspend fun getService(id: String): Outcome<Service> = 
            Outcome.Error(Failure.Unknown)
            
        override suspend fun submitReview(serviceId: String, rating: Int, comment: String): Outcome<Review> = 
            Outcome.Error(Failure.Unknown)
            
        override fun observeServices(filter: ServiceFilter): Flow<Outcome<List<Service>>> = 
            flowOf(Outcome.Success(emptyList()))
            
        override fun observeCategories(): Flow<Outcome<List<ServiceCategory>>> = 
            flowOf(Outcome.Success(listOf(ServiceCategory("1", "Cat 1", "icon", null))))
    }

    private class FakePromoRepository : PromoRepository {
        override suspend fun validatePromo(code: String, cartItems: List<CartItem>): Outcome<PromoValidation> = 
            Outcome.Error(Failure.Unknown)
        override fun observePromotions(): Flow<Outcome<List<Promotion>>> = 
            flowOf(Outcome.Success(emptyList()))
        override suspend fun fetchPromotions(): Outcome<List<Promotion>> = 
            Outcome.Success(emptyList())
        override suspend fun getUsedPromotionIds(): Outcome<List<String>> = 
            Outcome.Success(emptyList())
    }
}
