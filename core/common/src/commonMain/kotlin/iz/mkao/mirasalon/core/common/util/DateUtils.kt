package iz.mkao.mirasalon.core.common.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

object DateUtils {

    fun formatUpcomingDate(epochMillis: Long, currentEpochMillis: Long): String {
        val tz = TimeZone.currentSystemDefault()
        val appointmentInstant = Instant.fromEpochMilliseconds(epochMillis)
        val currentInstant = Instant.fromEpochMilliseconds(currentEpochMillis)
        
        val appointmentDate = appointmentInstant.toLocalDateTime(tz).date
        val currentDate = currentInstant.toLocalDateTime(tz).date
        
        val daysUntil = currentDate.daysUntil(appointmentDate)
        
        val timeStr = formatTime(epochMillis / 1000)
        
        return when (daysUntil) {
            0 -> "Today at $timeStr"
            1 -> "Tomorrow at $timeStr"
            in 2..6 -> "${appointmentDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} at $timeStr"
            else -> "${formatDateSeparator(epochMillis / 1000)} at $timeStr"
        }
    }

    fun formatBookedDate(epochMillis: Long, currentEpochMillis: Long): String {
        val tz = TimeZone.currentSystemDefault()
        val bookedInstant = Instant.fromEpochMilliseconds(epochMillis)
        val currentInstant = Instant.fromEpochMilliseconds(currentEpochMillis)
        
        val bookedDate = bookedInstant.toLocalDateTime(tz).date
        val currentDate = currentInstant.toLocalDateTime(tz).date
        
        val daysAgo = bookedDate.daysUntil(currentDate)
        
        return when (daysAgo) {
            0 -> "Booked Today"
            1 -> "Booked Yesterday"
            else -> "Booked on ${formatDateFull(epochMillis / 1000)}"
        }
    }
    
    fun formatTime(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return ""
        val instant = Instant.fromEpochSeconds(epochSeconds)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
        val amPm = if (dateTime.hour < 12) "AM" else "PM"
        return "${hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')} $amPm"
    }

    fun formatTime24Hour(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return ""
        val instant = Instant.fromEpochSeconds(epochSeconds)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.hour}:${dateTime.minute.toString().padStart(2, '0')}"
    }

    fun formatDateSeparator(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return ""
        val instant = Instant.fromEpochSeconds(epochSeconds)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        
        return "${dateTime.day} ${dateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
    }

    fun formatDateFull(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return "-"
        val instant = Instant.fromEpochSeconds(epochSeconds)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = dateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        return "${dateTime.day} $month ${dateTime.year}"
    }

    fun formatDateTime(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return "Recently"
        val instant = Instant.fromEpochSeconds(epochSeconds)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = dateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        return "${dateTime.day} $month ${dateTime.year}, $hour:$minute"
    }

    fun formatRelativeTime(epochMillis: Long, currentEpochMillis: Long): String {
        val diffMillis = currentEpochMillis - epochMillis
        val diffSeconds = diffMillis / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24
        
        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays < 7 -> "${diffDays}d ago"
            else -> formatDateSeparator(epochMillis / 1000)
        }
    }
}
