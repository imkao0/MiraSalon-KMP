package iz.mkao.mirasalon.core.domain.model

/**
 * Parameter Object Pattern: Future-proofing for new filters (minPrice, sortBy, etc.)
 */
data class ServiceFilter(
    val categoryId: String? = null,
    val searchQuery: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null
)
