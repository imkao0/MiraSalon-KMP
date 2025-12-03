package iz.mkao.mirasalon.feature.booking

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeZoneConversionTest {

    @Test
    fun testEpochToLocalDateTime() {
        val epochMillis = 1722369600000L // July 30, 2024 20:00:00 UTC
        val timeZone = TimeZone.of("UTC")
        val localDateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        
        assertEquals(2024, localDateTime.year)
        assertEquals(7, localDateTime.monthNumber)
        assertEquals(30, localDateTime.dayOfMonth)
        assertEquals(20, localDateTime.hour)
        assertEquals(0, localDateTime.minute)
    }

    @Test
    fun testTimeZoneDifference() {
        val epochMillis = 1722369600000L // July 30, 2024 20:00:00 UTC
        val utc = TimeZone.of("UTC")
        val ny = TimeZone.of("America/New_York") // UTC-4 in July
        
        val utcDateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(utc)
        val nyDateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(ny)
        
        assertEquals(20, utcDateTime.hour)
        assertEquals(16, nyDateTime.hour)
    }
}
