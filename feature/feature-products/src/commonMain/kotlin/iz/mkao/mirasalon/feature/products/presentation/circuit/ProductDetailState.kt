package iz.mkao.mirasalon.feature.products.presentation.circuit

import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.Review

data class ProductDetailState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val reviews: List<Review> = emptyList(),
    val isWishlisted: Boolean = false,
    val error: String? = null,
    val eventSink: (ProductDetailEvent) -> Unit = {},
) : CircuitUiState

sealed interface ProductDetailEvent {
    data object Back : ProductDetailEvent
    data class AddToCart(val productId: String, val quantity: Int) : ProductDetailEvent
    data class SubmitReview(val rating: Int, val comment: String) : ProductDetailEvent
    data object ToggleWishlist : ProductDetailEvent
}
