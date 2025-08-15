package iz.mkao.mirasalon.core.domain.model

/**
 * How a promotion discount is expressed (admin view).
 */
enum class AdminDiscountType {
    Percentage,
    FixedAmount,
    ;

    companion object {
        fun fromString(value: String?): AdminDiscountType =
            entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: Percentage
    }
}

/** Lifecycle status of a promotion, derived from its validity window. */
enum class AdminPromoStatus {
    Draft,
    Active,
    Scheduled,
    Expired,
    ;

    companion object {
        fun fromString(value: String?): AdminPromoStatus =
            entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: Draft
    }
}

/**
 * Admin-facing promotion model.
 */
data class AdminPromotion(
    val id: String,
    val code: String,
    val description: String = "",
    val discountType: AdminDiscountType = AdminDiscountType.Percentage,
    val discountValue: Double = 0.0,
    val minSpend: Double = 0.0,
    val validFrom: Long = 0L,
    val validUntil: Long? = null,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val totalRedemptions: Int? = null,
    val perUserRedemptions: Int = 1,
    val applicableServices: List<String> = emptyList(),
    val applicableCategories: List<String> = emptyList(),
    val status: AdminPromoStatus = AdminPromoStatus.Draft,
    val currentUsageCount: Int = 0,
    val minOrderValue: Double? = null,
    val type: PromoType = PromoType.EXPERTS
)
