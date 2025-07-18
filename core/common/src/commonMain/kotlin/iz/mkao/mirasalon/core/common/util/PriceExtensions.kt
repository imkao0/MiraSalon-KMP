package iz.mkao.mirasalon.core.common.util

import kotlin.math.abs
import kotlin.math.round

fun Double?.toPriceString(currency: String = "$"): String {
    if (this == null) return "${currency}0.00"
    
    val cents = round(this * 100.0).toLong()
    val absCents = abs(cents)
    val dollars = absCents / 100
    val remainder = absCents % 100
    
    val prefix = if (cents < 0) "-" else ""
    val centsStr = remainder.toString().padStart(2, '0')

    val dollarsStr = dollars.toString()
    val sb = StringBuilder()
    var count = 0
    for (i in dollarsStr.length - 1 downTo 0) {
        sb.append(dollarsStr[i])
        count++
        if (count % 3 == 0 && i > 0) {
            sb.append(",")
        }
    }
    
    return "$prefix$currency${sb.reverse()}.$centsStr"
}
