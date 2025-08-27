package iz.mkao.mirasalon.feature.salon.services.presentation.circuit

import androidx.compose.runtime.*
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.common.di.AppScope
import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.model.ServiceFilter
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.PromoRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceRepository
import iz.mkao.mirasalon.core.navigation.ServiceRoute

@CircuitInject(ServiceRoute.Services::class, AppScope::class)
class ServicesPresenter(
    private val screen: ServiceRoute.Services,
    private val repository: ServiceRepository,
    private val promoRepository: PromoRepository,
    private val navigator: Navigator
) : Presenter<ServicesState> {

    @Composable
    override fun present(): ServicesState {
        var isLoading by remember { mutableStateOf(false) }
        var services by remember { mutableStateOf(emptyList<iz.mkao.mirasalon.core.domain.model.Service>()) }
        var promotions by remember { mutableStateOf(emptyList<Promotion>()) }
        var selectedCategoryId by remember { mutableStateOf(screen.categoryId) }
        var searchQuery by remember { mutableStateOf("") }
        var sortOrder by remember { mutableStateOf(SortOrder.ASCENDING) }
        var error by remember { mutableStateOf<String?>(null) }

        val categoriesOutcome by repository.observeCategories().collectAsState(initial = Outcome.Loading)
        val categories = (categoriesOutcome as? Outcome.Success)?.data ?: emptyList()

        LaunchedEffect(Unit) {
            promoRepository.fetchPromotions().let {
                if (it is Outcome.Success) {
                    promotions = it.data
                }
            }
        }

        LaunchedEffect(selectedCategoryId, searchQuery, sortOrder) {
            if (searchQuery.isBlank() && selectedCategoryId == null) {
                services = emptyList()
                isLoading = false
                return@LaunchedEffect
            }
            
            isLoading = true
            val filter = ServiceFilter(categoryId = selectedCategoryId, searchQuery = searchQuery)
            repository.observeServices(filter).collect { result ->
                when (result) {
                    is Outcome.Success -> {
                        isLoading = false
                        services = if (sortOrder == SortOrder.ASCENDING) {
                            result.data.sortedBy { it.name }
                        } else {
                            result.data.sortedByDescending { it.name }
                        }
                    }
                    is Outcome.Error -> {
                        isLoading = false
                        error = "Failed to load services"
                    }
                    is Outcome.Loading -> isLoading = true
                }
            }
        }

        return ServicesState(
            isLoading = isLoading,
            services = services,
            promotions = promotions,
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            isCategoryFixed = screen.categoryId != null,
            searchQuery = searchQuery,
            sortOrder = sortOrder,
            error = error,
            eventSink = { event ->
                when (event) {
                    is ServicesEvent.CategorySelected -> selectedCategoryId = event.categoryId
                    is ServicesEvent.SearchQueryChanged -> searchQuery = event.query
                    is ServicesEvent.ServiceClicked -> {
                        navigator.goTo(ServiceRoute.ServiceDetail(serviceId = event.serviceId))
                    }
                    ServicesEvent.BackClicked -> navigator.pop()
                    ServicesEvent.Retry -> {
                        // Categories are observed, and services will be re-collected due to side effect if filter changes
                        // but here we can manually trigger a refresh if we had a manual trigger in repo
                    }
                    ServicesEvent.ToggleSortOrder -> {
                        sortOrder = if (sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
                    }
                }
            }
        )
    }
}
