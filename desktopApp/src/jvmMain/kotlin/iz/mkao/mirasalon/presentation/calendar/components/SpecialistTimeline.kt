package iz.mkao.mirasalon.presentation.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.RadiusMedium
import iz.mkao.mirasalon.core.domain.model.AdminAppointment
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStatus
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun SpecialistRowHeader(specialist: Specialist) {
    Row(
        modifier = Modifier
            .fillMaxWidth()

            .height(80.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val initials = specialist.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2)

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MiraCoral),
            contentAlignment = Alignment.Center
        ) {
            val imageUrl = specialist.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                val fullUrl = ApiEndpoints.resolveImageUrl(imageUrl)
                
                AsyncImage(
                    model = fullUrl,
                    contentDescription = specialist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = {
                         Napier.e(it.result.throwable) { "Coil failed to load calendar specialist image: $fullUrl" }
                    }
                )
            } else {
                Text(
                    initials,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                specialist.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MiraTextPrimary
            )
            if (specialist.isActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Active",
                        fontSize = 12.sp,
                        color = MiraTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun AppointmentBlock(
    appt: AdminAppointment,
    onClick: () -> Unit,
    onStatusUpdate: ((AdminAppointment, AdminAppointmentStatus) -> Unit)? = null
) {
    var showQuickActions by remember { mutableStateOf(false) }

    val (bgColor, textColor, borderColor) = when (appt.status) {
        AdminAppointmentStatus.Confirmed -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        AdminAppointmentStatus.Completed -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
        AdminAppointmentStatus.Cancelled -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.outline)
    }


    val time = Instant.fromEpochMilliseconds(appt.dateTime).toLocalDateTime(TimeZone.currentSystemDefault())
    val endTime = Instant.fromEpochMilliseconds(appt.dateTime + appt.durationMinutes * 60 * 1000).toLocalDateTime(TimeZone.currentSystemDefault())
    val timeRange = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')} - ${endTime.hour.toString().padStart(2, '0')}:${endTime.minute.toString().padStart(2, '0')}"

    Surface(
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .fillMaxHeight()
            .clickable {
                if (onStatusUpdate != null) {
                    showQuickActions = true
                } else {
                    onClick()
                }
            },
        shape = RoundedCornerShape(RadiusMedium),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                appt.customerName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1
            )
            Text(
                appt.serviceNames.firstOrNull() ?: "Service",
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.8f),
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    null,
                    modifier = Modifier.size(12.dp),
                    tint = textColor.copy(alpha = 0.8f)
                )
                Text(
                    timeRange,
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        if (showQuickActions && onStatusUpdate != null) {
            DropdownMenu(
                expanded = showQuickActions,
                onDismissRequest = { showQuickActions = false },
                modifier = Modifier.background(Color.White)
            ) {
                Text(
                    "Update Status",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MiraTextSecondary
                )

                if (appt.status == AdminAppointmentStatus.Confirmed) {
                    DropdownMenuItem(
                        text = { Text("Mark Completed") },
                        leadingIcon = { Icon(Icons.Outlined.Check, null) },
                        onClick = {
                            onStatusUpdate(appt, AdminAppointmentStatus.Completed)
                            showQuickActions = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cancel Appointment") },
                        leadingIcon = { Icon(Icons.Outlined.Close, null) },
                        onClick = {
                            onStatusUpdate(appt, AdminAppointmentStatus.Cancelled)
                            showQuickActions = false
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("View Details") },
                    onClick = {
                        onClick()
                        showQuickActions = false
                    }
                )
            }
        }
    }
}
