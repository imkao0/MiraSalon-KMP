package iz.mkao.mirasalon.feature.favourites.salon.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.navigation.ProductRoute
import iz.mkao.mirasalon.core.navigation.ProfileRoute
import iz.mkao.mirasalon.core.navigation.ServiceRoute
import iz.mkao.mirasalon.core.domain.repository.FavouritesRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceFavouritesRepository
import iz.mkao.mirasalon.feature.favourites.domain.model.FavouritesState as DomainFavouritesState
import iz.mkao.mirasalon.feature.favourites.domain.usecase.GetFavouritesUseCase
import kotlinx.coroutines.launch

@Composable
fun favouritesPresenter(
    navigator: Navigator,
    getFavouritesUseCase: GetFavouritesUseCase,
    favouritesRepository: FavouritesRepository,
    serviceFavouritesRepository: ServiceFavouritesRepository
): FavouritesState {
    val domainState by getFavouritesUseCase().collectAsState(initial = DomainFavouritesState())
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    return FavouritesState(
        products = domainState.products,
        services = domainState.services,
        eventSink = { event ->
            when (event) {
                is FavouritesEvent.ProductClicked -> navigator.goTo(ProductRoute.ProductDetail(productId = event.id))
                is FavouritesEvent.ServiceClicked -> {
                    navigator.goTo(ServiceRoute.ServiceDetail(serviceId = event.id))
                }
                is FavouritesEvent.RemoveProductFavorite -> {
                    scope.launch {
                        favouritesRepository.removeProductFromFavourites(event.id)
                    }
                }
                is FavouritesEvent.RemoveServiceFavorite -> {
                    scope.launch {
                        serviceFavouritesRepository.removeServiceFromFavourites(event.id)
                    }
                }
                FavouritesEvent.BackClicked -> navigator.pop()
            }
        }
    )
}

class FavouritesManualPresenterFactory(
    private val getFavouritesUseCase: GetFavouritesUseCase,
    private val favouritesRepository: FavouritesRepository,
    private val serviceFavouritesRepository: ServiceFavouritesRepository
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is ProfileRoute.Favourites -> object : Presenter<FavouritesState> {
                @Composable override fun present() = favouritesPresenter(navigator, getFavouritesUseCase, favouritesRepository, serviceFavouritesRepository)
            }
            else -> null
        }
    }
}