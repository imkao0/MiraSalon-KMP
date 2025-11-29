package iz.mkao.mirasalon.core.domain.model

/** A single selectable option for a product variant (e.g. "Size: 50ml"). */
data class AdminProductVariantOption(
    val id: String,
    val name: String,
    val value: String,
    val priceDelta: Double = 0.0,
)

/**
 * Admin-facing product model used by the inventory screen.
 */
data class AdminProduct(
    val id: String,
    val name: String,
    val description: String = "",
    val brand: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val price: Double = 0.0,
    val stockQuantity: Int = 0,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val lowStockThreshold: Int = 0,
    val variants: List<AdminProductVariantOption> = emptyList(),
) {
    /** True when stock has fallen to (or below) the configured threshold. */
    val isLowStock: Boolean
        get() = lowStockThreshold > 0 && stockQuantity <= lowStockThreshold
}

/** Grouping for services offered by the salon. */
data class AdminServiceCategory(
    val id: String,
    val name: String,
    val description: String = "",
    val imageUrl: String? = null,
)

/**
 * Admin-facing service model (richer than the customer-side [Service]).
 */
data class AdminService(
    val id: String,
    val name: String,
    val description: String = "",
    val price: Double = 0.0,
    val durationMinutes: Int = 0,
    val imageUrl: String? = null,
    val categoryId: String = "",
    val salonId: String = "",
    val subCategory: String = "",
    val rating: Double = 0.0,
    val isActive: Boolean = true,
)
