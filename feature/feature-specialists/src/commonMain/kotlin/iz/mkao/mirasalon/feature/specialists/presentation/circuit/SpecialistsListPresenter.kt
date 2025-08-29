package iz.mkao.mirasalon.feature.specialists.presentation.circuit

import androidx.compose.runtime.*
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.navigation.SpecialistRoute
import kotlinx.coroutines.launch

class SpecialistsListPresenter(
    private val repository: SpecialistRepository,
    private val navigator: Navigator,
) : Presenter<SpecialistsState> {

    @Composable
    override fun present(): SpecialistsState {
        var isLoading by remember { mutableStateOf(true) }
        var isRefreshing by remember { mutableStateOf(false) }
        var specialists by remember { mutableStateOf(emptyList<Specialist>()) }
        var searchQuery by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var retryCount by remember { mutableIntStateOf(0) }

        LaunchedEffect(retryCount) {
            repository.observeSpecialists().collect { outcome ->
                when (outcome) {
                    is Outcome.Success -> {
                        isLoading = false
                        isRefreshing = false
                        specialists = outcome.data
                        error = null
                    }
                    is Outcome.Error -> {
                        isLoading = false
                        isRefreshing = false
                        error = "Failed to load specialists"
                    }
                    is Outcome.Loading -> {
                        if (!isRefreshing) isLoading = true
                    }
                }
            }
        }

        val filteredSpecialists = remember(specialists, searchQuery) {
            if (searchQuery.isBlank()) {
                specialists
            } else {
                specialists.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.role.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        val scope = rememberCoroutineScope()

        return SpecialistsState(
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            searchQuery = searchQuery,
            specialists = specialists,
            filteredSpecialists = filteredSpecialists,
            error = error,
            eventSink = { event ->
                when (event) {
                    is SpecialistsEvent.SpecialistClicked ->
                        navigator.goTo(SpecialistRoute.SpecialistDetail(specialistId = event.specialistId))
                    is SpecialistsEvent.BookSpecialistClicked ->
                        navigator.goTo(SpecialistRoute.SpecialistDetail(specialistId = event.specialistId))
                    SpecialistsEvent.Back -> navigator.pop()
                    SpecialistsEvent.Retry -> retryCount++
                    SpecialistsEvent.Refresh -> {
                        isRefreshing = true
                        scope.launch { repository.refresh() }
                    }
                    is SpecialistsEvent.SearchQueryChanged -> searchQuery = event.query
                }
            },
        )
    }
}

class SpecialistsManualPresenterFactory(
    private val repository: SpecialistRepository,
    private val tokenProvider: iz.mkao.mirasalon.core.network.client.SalonTokenProvider
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is SpecialistRoute.Specialists -> SpecialistsListPresenter(repository, navigator)
            is SpecialistRoute.SpecialistDetail -> SpecialistDetailPresenter(screen, repository, navigator, tokenProvider)
            else -> null
        }
    }
}
