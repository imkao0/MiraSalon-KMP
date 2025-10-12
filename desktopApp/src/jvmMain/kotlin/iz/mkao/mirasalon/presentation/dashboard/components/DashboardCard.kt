package iz.mkao.mirasalon.presentation.dashboard.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.RadiusLarge

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    timeLabel: String = "7 days",
    topRightContent: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = MiraTextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(timeLabel, fontSize = 12.sp, color = MiraTextSecondary)
        }
    },
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.border(1.dp, MiraBorder.copy(alpha = 0.5f), RoundedCornerShape(RadiusLarge)),
        color = Color.White,
        shape = RoundedCornerShape(RadiusLarge)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Outlined.Info, null, tint = MiraTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MiraTextSecondary)
                }
                topRightContent()
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MiraBorder.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))
            content()
        }
    }
}
