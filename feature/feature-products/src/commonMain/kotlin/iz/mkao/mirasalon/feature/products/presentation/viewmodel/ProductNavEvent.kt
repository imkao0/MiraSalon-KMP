package iz.mkao.mirasalon.feature.products.presentation.viewmodel

sealed interface ProductNavEvent {
    data object NavigateToCart : ProductNavEvent
}
