package iz.mkao.mirasalon.core.domain.model

data class ProductPage(
    val products: List<Product>,
    val subCategories: List<String>,
    val hasMore: Boolean,
)
