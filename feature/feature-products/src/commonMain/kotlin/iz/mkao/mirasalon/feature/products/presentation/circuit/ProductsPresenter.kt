package iz.mkao.mirasalon.feature.products.presentation.circuit

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.domain.repository.CartRepository
import iz.mkao.mirasalon.core.domain.repository.ProductRepository
import iz.mkao.mirasalon.core.domain.repository.FavouritesRepository
import iz.mkao.mirasalon.core.domain.repository.PromoRepository
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.ProductRoute

class ProductsManualPresenterFactory(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val favouritesRepository: FavouritesRepository,
    private val promoRepository: PromoRepository
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is BottomNavKey.Discover -> ExploreCategoriesPresenter(null, productRepository, cartRepository, promoRepository, navigator)
            is ProductRoute.Products -> ExploreCategoriesPresenter(screen.category, productRepository, cartRepository, promoRepository, navigator)
            is ProductRoute.ExploreCategories -> ExploreCategoriesPresenter(null, productRepository, cartRepository, promoRepository, navigator)
            is ProductRoute.ProductDetail -> ProductDetailPresenter(screen, productRepository, cartRepository, favouritesRepository, navigator)
            else -> null
        }
    }
}
