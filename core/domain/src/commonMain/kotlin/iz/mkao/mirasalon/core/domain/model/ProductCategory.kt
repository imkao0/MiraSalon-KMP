package iz.mkao.mirasalon.core.domain.model

data class ProductCategory(
    val id: String,
    val name: String,
    val imageUrl: String,
    val productCount: Int = 0,
)
