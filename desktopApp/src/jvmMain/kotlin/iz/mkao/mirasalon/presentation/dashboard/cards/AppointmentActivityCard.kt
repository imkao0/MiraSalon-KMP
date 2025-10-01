package iz.mkao.mirasalon.presentation.dashboard.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraGreen
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.MiraYellow
import iz.mkao.mirasalon.core.designsystem.theme.RadiusExtraSmall
import iz.mkao.mirasalon.core.domain.model.ActivityEvent
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AppointmentActivityCard(
    modifier: Modifier = Modifier,
    activities: List<ActivityEvent> = emptyList()
) {
    DashboardCard(
        modifier = modifier,
        title = "Appointment activity",
        subtitle = "Stay updated with real-time appointment",
        topRightContent = {
            Icon(Icons.Outlined.NorthEast, null, tint = MiraTextSecondary, modifier = Modifier.size(18.dp))
        }
    ) {
        val filteredActivities = activities.filter { it.type == "APPOINTMENT" }

        if (filteredActivities.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("No recent activity", color = MiraTextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                filteredActivities.forEachIndexed { index, activity ->
                    ActivityItem(activity)
                    if (index < filteredActivities.size - 1) {
                        HorizontalDivider(color = MiraBorder.copy(alpha = 0.5f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: ActivityEvent) {
    val zonedDateTime = try {
        Instant.ofEpochMilli(activity.timestamp.toLong()).atZone(ZoneId.systemDefault())
    } catch (e: Exception) {
        null
    }

    val month = zonedDateTime?.month?.getDisplayName(TextStyle.SHORT, Locale.getDefault())?.uppercase() ?: "---"
    val day = zonedDateTime?.dayOfMonth?.toString() ?: "--"
    val time = if (zonedDateTime != null) {
        val hour = zonedDateTime.hour % 12
        val amPm = if (zonedDateTime.hour >= 12) "pm" else "am"
        "${if (hour == 0) 12 else hour}:${zonedDateTime.minute.toString().padStart(2, '0')} $amPm"
    } else ""

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Column(
            modifier = Modifier.width(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(month, fontSize = 10.sp, color = MiraTextSecondary, fontWeight = FontWeight.Bold)
            Text(day, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
        }


        Surface(
            modifier = Modifier.size(32.dp).clip(CircleShape),
            color = Color.White,
            border = BorderStroke(1.dp, MiraBorder)
        ) {
            if (activity.imageUrl != null) {
                AsyncImage(
                    model = ApiEndpoints.resolveImageUrl(activity.imageUrl),
                    contentDescription = "Specialist",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Person, null, tint = MiraTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(activity.message, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 13.sp)
            Text("$month $day, $time | ${activity.customerEmail}", color = MiraTextSecondary, fontSize = 11.sp)
        }


        Surface(
            color = when(activity.status) {
                "CONFIRMED", "BOOKED", "COMPLETED" -> MiraGreen.copy(alpha = 0.15f)
                "CANCELLED" -> MiraCoral.copy(alpha = 0.15f)
                else -> MiraYellow.copy(alpha = 0.15f)
            },
            shape = RoundedCornerShape(RadiusExtraSmall),
        ) {
            Text(
                activity.status.lowercase().replaceFirstChar { it.uppercase() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = when(activity.status) {
                    "CONFIRMED", "BOOKED", "COMPLETED" -> MiraGreen
                    "CANCELLED" -> MiraCoral
                    else -> MiraYellow
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
