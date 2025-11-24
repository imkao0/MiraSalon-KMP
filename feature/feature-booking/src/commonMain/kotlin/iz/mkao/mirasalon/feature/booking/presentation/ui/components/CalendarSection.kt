package iz.mkao.mirasalon.feature.booking.presentation.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.theme.*
import iz.mkao.mirasalon.feature.booking.presentation.circuit.BookingState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@Composable
fun CalendarSection(
    state: BookingState,
    onDateSelected: (LocalDate) -> Unit,
    onToggle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val selected = state.selectedDate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected != null) {
                Text(
                    text = selected.month.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (state.calendarExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (state.calendarExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        WeekDayHeader()

        Spacer(modifier = Modifier.height(4.dp))

        ExpandedCalendarGrid(
            days = state.days,
            selectedDate = state.selectedDate,
            datesWithBookings = state.datesWithBookings,
            expanded = state.calendarExpanded,
            onDateSelected = onDateSelected
        )
    }
}


@Composable
private fun WeekDayHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        days.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ExpandedCalendarGrid(
    days: List<LocalDate>,
    selectedDate: LocalDate?,
    datesWithBookings: Set<String>,
    expanded: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    val displayedDays = if (expanded) days else days.take(7)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            displayedDays.chunked(7).forEachIndexed { index, rowDays ->
                val shouldFade = index >= 1 && !expanded

                Box {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowDays.forEach { date ->
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                DateCell(
                                    date = date,
                                    isSelected = date == selectedDate,
                                    hasBookings = date.toString() in datesWithBookings,
                                    onClick = { onDateSelected(date) }
                                )
                            }
                        }
                        repeat(7 - rowDays.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    if (shouldFade) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    hasBookings: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    val border = if (isSelected) null
    else BorderStroke(1.dp, MiraBorder)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        Card(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = bg,
                contentColor = contentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = border
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                    if (hasBookings) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}
