package iz.mkao.mirasalon.core.common.util

import kotlin.math.round

fun Double.formatRating(): String {
    val rounded = round(this * 10) / 10.0
    return if (rounded % 1.0 == 0.0) "${rounded.toInt()}.0" else rounded.toString()
}
