package iz.mkao.mirasalon.feature.products.presentation.circuit

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.ProductRoute

// Explicit imports for clarity and to ensure connection
import iz.mkao.mirasalon.feature.products.presentation.circuit.ExploreCategoriesContent
import iz.mkao.mirasalon.feature.products.presentation.circuit.ExploreCategoriesState
import iz.mkao.mirasalon.feature.products.presentation.circuit.ProductDetailContent
import iz.mkao.mirasalon.feature.products.presentation.circuit.ProductDetailState

class ProductsManualUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is BottomNavKey.Discover -> ui<ExploreCategoriesState> { state, modifier -> ExploreCategoriesContent(state, modifier) }
            is ProductRoute.Products -> ui<ExploreCategoriesState> { state, modifier -> ExploreCategoriesContent(state, modifier) }
            is ProductRoute.ExploreCategories -> ui<ExploreCategoriesState> { state, modifier -> ExploreCategoriesContent(state, modifier) }
            is ProductRoute.ProductDetail -> ui<ProductDetailState> { state, modifier -> ProductDetailContent(state, modifier) }
            else -> null
        }
    }
}
