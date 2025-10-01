package iz.mkao.mirasalon.presentation.dashboard.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.designsystem.theme.*
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardCard

@Composable
fun PaymentBreakdownCard(
    modifier: Modifier = Modifier
) {
    DashboardCard(modifier, title = "Payment breakdown", subtitle = "Track Your Latest Sales") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PaymentRow(label = "Succeeded", count = 14, progress = 0.8f, color = MiraCoral)
            PaymentRow(label = "Refunded", count = 0, progress = 0.0f, color = Color.LightGray.copy(alpha = 0.5f))
            PaymentRow(label = "Failed", count = 0, progress = 0.0f, color = Color.LightGray.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun PaymentRow(label: String, count: Int, progress: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("$count", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = Color(0xFFF5F5F5),
            strokeCap = StrokeCap.Round
        )
    }
}
