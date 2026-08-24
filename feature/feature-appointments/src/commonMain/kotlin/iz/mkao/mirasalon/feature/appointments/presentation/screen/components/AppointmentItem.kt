package iz.mkao.mirasalon.feature.appointments.presentation.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.theme.Cancelled
import iz.mkao.mirasalon.core.designsystem.theme.ElevationLow
import iz.mkao.mirasalon.core.designsystem.theme.RadiusSmall
import iz.mkao.mirasalon.core.designsystem.theme.SpacingDefault
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall
import iz.mkao.mirasalon.core.designsystem.theme.SpacingTiny
import iz.mkao.mirasalon.core.designsystem.theme.Success
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.feature.appointments.domain.model.Appointment
import kotlinx.datetime.*

@Composable
fun AppointmentItem(
    appointment: Appointment,
    currentTimeMillis: Long,
    onClick: () -> Unit,
    onSpecialistClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingSmall)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusSmall),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = ElevationLow)
    ) {
        Row(
            modifier = Modifier
                .padding(SpacingMedium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.salonName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Specialist: ${appointment.specialistName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onSpecialistClick(appointment.specialistId) }
                )
                Spacer(modifier = Modifier.height(SpacingTiny))
                Text(
                    text = appointment.services.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(SpacingTiny))
                Text(
                    text = DateUtils.formatUpcomingDate(appointment.dateTime, currentTimeMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            StatusChip(status = appointment.status.name)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        "Confirmed" -> Success
        "Completed" -> MaterialTheme.colorScheme.primary
        "Cancelled" -> Cancelled
        else -> MaterialTheme.colorScheme.outline
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(RadiusSmall))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = SpacingDefault, vertical = SpacingTiny)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
