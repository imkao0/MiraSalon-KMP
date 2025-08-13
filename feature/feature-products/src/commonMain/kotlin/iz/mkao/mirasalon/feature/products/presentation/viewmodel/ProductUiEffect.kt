package iz.mkao.mirasalon.feature.products.presentation.viewmodel

sealed interface ProductUiEffect {
    data class ShowSnackbar(val message: String) : ProductUiEffect
}
