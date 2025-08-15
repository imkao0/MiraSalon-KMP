package iz.mkao.mirasalon.feature.products.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.model.Cart
import iz.mkao.mirasalon.core.domain.model.CartItem
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.ProductVariation
import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.CartRepository
import iz.mkao.mirasalon.core.domain.repository.ProductRepository
import iz.mkao.mirasalon.core.domain.repository.PromoRepository
import iz.mkao.mirasalon.core.navigation.CartRoute
import iz.mkao.mirasalon.core.navigation.ProductRoute
import kotlinx.coroutines.launch

class ExploreCategoriesPresenter(
    private val initialCategoryId: String? = null,
    private val repository: ProductRepository,
    private val cartRepository: CartRepository,
    private val promoRepository: PromoRepository,
    private val navigator: Navigator
) : Presenter<ExploreCategoriesState> {

    @Composable
    override fun present(): ExploreCategoriesState {
        var categories by remember { mutableStateOf(emptyList<ProductCategory>()) }
        var products by remember { mutableStateOf(emptyList<Product>()) }
        var promotions by remember { mutableStateOf(emptyList<Promotion>()) }
        var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
        var selectedVariation by remember { mutableStateOf<ProductVariation?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        var isRefreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val cart by cartRepository.observeCart().collectAsState(initial = Cart())

        fun loadData() {
            scope.launch {
                if (!isRefreshing) isLoading = true
                
                val prodsResult = repository.getProducts(
                    category = selectedCategory?.id?.takeIf { it.isNotEmpty() } ?: selectedCategory?.name,
                    subCategory = null,
                    page = 1,
                    pageSize = 50,
                    query = null,
                    gender = selectedVariation?.name
                )
                if (prodsResult is Outcome.Success) {
                    products = prodsResult.data.products
                }
                isLoading = false
                isRefreshing = false
            }
        }

        LaunchedEffect(Unit) {
            val catsResult = repository.getCategories()
            if (catsResult is Outcome.Success) {
                categories = catsResult.data
                if (selectedCategory == null && initialCategoryId != null) {
                    selectedCategory = categories.find { it.id == initialCategoryId }
                }
            }
            promoRepository.fetchPromotions().let {
                if (it is Outcome.Success) {
                    promotions = it.data
                }
            }
        }

        LaunchedEffect(selectedCategory, selectedVariation) {
            loadData()
        }

        return ExploreCategoriesState(
            categories = categories,
            products = products,
            promotions = promotions,
            selectedCategory = selectedCategory,
            selectedVariation = selectedVariation,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            cartItemCount = cart.itemCount,
            eventSink = { event ->
                when (event) {
                    ExploreCategoriesEvent.Back -> navigator.pop()
                    ExploreCategoriesEvent.Refresh -> {
                        isRefreshing = true
                        loadData()
                    }
                    is ExploreCategoriesEvent.CategorySelected -> {
                        selectedCategory = event.category
                    }
                    is ExploreCategoriesEvent.VariationSelected -> {
                        selectedVariation = event.variation
                    }
                    is ExploreCategoriesEvent.ProductClicked -> {
                        navigator.goTo(ProductRoute.ProductDetail(event.productId))
                    }
                    is ExploreCategoriesEvent.AddToCart -> {
                        products.find { it.id == event.productId }?.let { product ->
                            scope.launch {
                                cartRepository.addToCart(CartItem(product = product, quantity = 1))
                            }
                        }
                    }
                    ExploreCategoriesEvent.CartClicked -> {
                        navigator.goTo(CartRoute.Cart)
                    }
                }
            }
        )
    }
}
