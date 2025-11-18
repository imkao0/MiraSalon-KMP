package iz.mkao.mirasalon.di

import iz.mkao.mirasalon.core.common.util.toPriceString

/**
 * Single source of truth for money formatting shared with SwiftUI.
 * iOS totals use the exact rounding semantics of the shared Kotlin code
 * (`toPriceString`) instead of a reimplemented Swift formatter that can
 * drift by a cent across platforms.
 */
internal object PriceFormatter {
    fun format(amount: Double, currency: String = "$"): String =
        amount.toPriceString(currency)

    fun formatNullable(amount: Double?, currency: String = "$"): String =
        amount.toPriceString(currency)
}
