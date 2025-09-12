package iz.mkao.mirasalon.feature.favourites.salon.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.navigation.ProfileRoute
import iz.mkao.mirasalon.feature.favourites.salon.presentation.screen.FavouritesContent

@Composable
fun FavouritesUi(state: FavouritesState, modifier: Modifier = Modifier) {
    FavouritesContent(
        products = state.products,
        services = state.services,
        onProductClick = { state.eventSink(FavouritesEvent.ProductClicked(it)) },
        onServiceClick = { state.eventSink(FavouritesEvent.ServiceClicked(it)) },
        onRemoveProductFavorite = { state.eventSink(FavouritesEvent.RemoveProductFavorite(it)) },
        onRemoveServiceFavorite = { state.eventSink(FavouritesEvent.RemoveServiceFavorite(it)) },
        onBackClick = { state.eventSink(FavouritesEvent.BackClicked) },
    )
}

class FavouritesManualUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is ProfileRoute.Favourites -> ui<FavouritesState> { state, modifier -> FavouritesUi(state, modifier) }
            else -> null
        }
    }
}