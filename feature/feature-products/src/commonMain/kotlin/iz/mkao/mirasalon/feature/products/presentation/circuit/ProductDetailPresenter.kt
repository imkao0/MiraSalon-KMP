package iz.mkao.mirasalon.feature.products.presentation.circuit

import androidx.compose.runtime.*
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.Review
import iz.mkao.mirasalon.core.domain.model.CartItem
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.ProductRepository
import iz.mkao.mirasalon.core.domain.repository.CartRepository
import iz.mkao.mirasalon.core.domain.repository.FavouritesRepository
import iz.mkao.mirasalon.core.navigation.CartRoute
import iz.mkao.mirasalon.core.navigation.ProductRoute
import kotlinx.coroutines.launch

class ProductDetailPresenter(
    private val screen: ProductRoute.ProductDetail,
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val favouritesRepository: FavouritesRepository,
    private val navigator: Navigator,
) : Presenter<ProductDetailState> {

    @Composable
    override fun present(): ProductDetailState {
        var isLoading by remember { mutableStateOf(true) }
        var product by remember { mutableStateOf<Product?>(null) }
        var reviews by remember { mutableStateOf(emptyList<Review>()) }
        var isWishlisted by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(screen.productId) {
            val productResult = productRepository.getProduct(screen.productId)
            if (productResult is Outcome.Success) {
                product = productResult.data
                isWishlisted = favouritesRepository.isFavourite(screen.productId)
                
                val reviewResult = productRepository.getReviews(screen.productId)
                if (reviewResult is Outcome.Success) {
                    reviews = reviewResult.data
                }
                isLoading = false
                error = null
            } else {
                isLoading = false
                error = "Product not found"
            }
        }

        return ProductDetailState(
            isLoading = isLoading,
            product = product,
            reviews = reviews,
            isWishlisted = isWishlisted,
            error = error,
            eventSink = { event ->
                when (event) {
                    ProductDetailEvent.Back -> navigator.pop()
                    ProductDetailEvent.ToggleWishlist -> {
                        product?.let { p ->
                            scope.launch {
                                if (isWishlisted) {
                                    favouritesRepository.removeProductFromFavourites(p.id)
                                } else {
                                    favouritesRepository.addProductToFavourites(p)
                                }
                                isWishlisted = !isWishlisted
                            }
                        }
                    }
                    is ProductDetailEvent.AddToCart -> {
                        product?.let { p ->
                            scope.launch {
                                cartRepository.addToCart(CartItem(product = p, quantity = event.quantity))
                                navigator.goTo(CartRoute.Cart)
                            }
                        }
                    }
                    is ProductDetailEvent.SubmitReview -> {
                        scope.launch {
                            productRepository.submitReview(screen.productId, event.rating, event.comment)
                            // Re-fetch reviews
                            val reviewResult = productRepository.getReviews(screen.productId)
                            if (reviewResult is Outcome.Success) {
                                reviews = reviewResult.data
                            }
                        }
                    }
                }
            }
        )
    }
}

