package iz.mkao.mirasalon.feature.booking.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.*
import iz.mkao.mirasalon.feature.booking.domain.model.BookingTimeSlot
import iz.mkao.mirasalon.feature.booking.presentation.circuit.BookingState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TimeSlotSection(
    state: BookingState,
    onSlotSelected: (BookingTimeSlot) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Select Time",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoadingSlots -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            ShimmerLoading()
                        }
                    }
                }
            }
            state.timeSlots.isEmpty() -> {
                Text(
                    text = "No available time slots for this date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                val timeZone = TimeZone.UTC
                val slotsWithHour = state.timeSlots.map { slot ->
                    val hour = Instant.fromEpochMilliseconds(slot.startTime)
                        .toLocalDateTime(timeZone).hour
                    slot to hour
                }

                val morning = slotsWithHour.filter { it.second in 0..11 }.map { it.first }
                val afternoon = slotsWithHour.filter { it.second in 12..16 }.map { it.first }
                val evening = slotsWithHour.filter { it.second in 17..23 }.map { it.first }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (morning.isNotEmpty()) {
                        SlotGroup(title = "Morning", slots = morning, selectedSlot = state.selectedSlot, onSlotSelected = onSlotSelected)
                    }
                    if (afternoon.isNotEmpty()) {
                        SlotGroup(title = "Afternoon", slots = afternoon, selectedSlot = state.selectedSlot, onSlotSelected = onSlotSelected)
                    }
                    if (evening.isNotEmpty()) {
                        SlotGroup(title = "Evening", slots = evening, selectedSlot = state.selectedSlot, onSlotSelected = onSlotSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotGroup(
    title: String,
    slots: List<BookingTimeSlot>,
    selectedSlot: BookingTimeSlot?,
    onSlotSelected: (BookingTimeSlot) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        slots.chunked(4).forEach { rowSlots ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowSlots.forEach { slot ->
                    Box(modifier = Modifier.weight(1f)) {
                        TimeSlotChip(
                            slot = slot,
                            isSelected = slot == selectedSlot,
                            onClick = { onSlotSelected(slot) }
                        )
                    }
                }
                repeat(4 - rowSlots.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TimeSlotChip(
    slot: BookingTimeSlot,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        !slot.isAvailable -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !slot.isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val border = when {
        isSelected -> null
        slot.isAvailable -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        else -> null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = slot.isAvailable, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = border
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = slot.formattedTime,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center
            )
        }
    }
}
