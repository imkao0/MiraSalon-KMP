package iz.mkao.mirasalon.core.domain.util

import iz.mkao.mirasalon.core.domain.model.DiscountType
import iz.mkao.mirasalon.core.domain.model.PromoStatus
import iz.mkao.mirasalon.core.domain.model.Promotion
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class PromotionValidationResult(
    val isValid: Boolean,
    val discountAmount: Double = 0.0,
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountValue: Double = 0.0,
    val finalTotal: Double = 0.0,
    val message: String = "",
    val errorMessage: String? = null,
    val promoCode: String = "",
    val applicableServices: List<String>? = null,
    val applicableCategories: List<String>? = null
)

object PromotionValidator {
    @ExperimentalTime
    fun validate(
        promo: Promotion,
        cartTotal: Double,
        userId: String?,
        serviceIds: List<String>? = null,
        categoryIds: List<String>? = null,
        userUsageCount: Int = 0,
        now: Instant
    ): PromotionValidationResult {
        // 1. Basic Status Check
        if (promo.status != PromoStatus.ACTIVE) {
            return PromotionValidationResult(isValid = false, errorMessage = "Promotion is not active", promoCode = promo.code ?: "")
        }

        // 2. Date Validation
        promo.validFrom?.let {
            if (now < it) return PromotionValidationResult(isValid = false, errorMessage = "Promotion hasn't started yet", promoCode = promo.code ?: "")
        }
        promo.validUntil?.let {
            if (now > it) return PromotionValidationResult(isValid = false, errorMessage = "Promotion has expired", promoCode = promo.code ?: "")
        }

        // 3. Usage Limits
        promo.usageLimit.totalRedemptions?.let {
            if (promo.currentUsageCount >= it) {
                return PromotionValidationResult(isValid = false, errorMessage = "Promotion usage limit reached", promoCode = promo.code ?: "")
            }
        }
        if (userUsageCount >= promo.usageLimit.perUserRedemptions) {
            return PromotionValidationResult(isValid = false, errorMessage = "You have already used this promotion the maximum number of times", promoCode = promo.code ?: "")
        }

        // 4. Target User Check
        if (promo.targetUserId != null && promo.targetUserId != userId) {
            return PromotionValidationResult(isValid = false, errorMessage = "This promotion is not available for your account", promoCode = promo.code ?: "")
        }

        // 5. Min Order Value
        promo.minOrderValue?.let {
            if (cartTotal < it) {
                return PromotionValidationResult(isValid = false, errorMessage = "Minimum order value of $it required", promoCode = promo.code ?: "")
            }
        }

        // 6. Applicable Services and Categories
        val isTargeted = !promo.applicableServices.isNullOrEmpty() || !promo.applicableCategories.isNullOrEmpty()
        if (isTargeted) {
            val matchesService = serviceIds?.any { it in (promo.applicableServices ?: emptyList()) } ?: false
            val matchesCategory = categoryIds?.any { it in (promo.applicableCategories ?: emptyList()) } ?: false

            if (!matchesService && !matchesCategory) {
                return PromotionValidationResult(
                    isValid = false,
                    errorMessage = "Promotion does not apply to selected services or categories",
                    promoCode = promo.code ?: ""
                )
            }
        }

        // 7. Calculate Discount
        var discountAmount = 0.0
        discountAmount = when (promo.discountType) {
            DiscountType.PERCENTAGE -> {
                cartTotal * (promo.discountValue / 100.0)
            }

            DiscountType.FIXED -> {
                promo.discountValue
            }

            DiscountType.FREE_SERVICE -> {
                // FREE_SERVICE grants a service rather than a monetary discount,
                // so there is no amount to subtract from the cart total.
                0.0
            }
        }

        val finalTotal = (cartTotal - discountAmount).coerceAtLeast(0.0)

        return PromotionValidationResult(
            isValid = true,
            discountAmount = discountAmount,
            discountType = promo.discountType,
            discountValue = promo.discountValue,
            finalTotal = finalTotal,
            message = "Promotion applied successfully",
            promoCode = promo.code ?: "",
            applicableServices = promo.applicableServices,
            applicableCategories = promo.applicableCategories
        )
    }
}
