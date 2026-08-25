package iz.mkao.mirasalon.core.network.util

import iz.mkao.mirasalon.core.common.util.toPriceString as commonToPriceString

/**
 * Formats a Double value as a price string.
 * Delegates to common implementation to ensure consistency.
 */
fun Double?.toPriceString(currency: String = "$"): String {
    return this.commonToPriceString(currency)
}
