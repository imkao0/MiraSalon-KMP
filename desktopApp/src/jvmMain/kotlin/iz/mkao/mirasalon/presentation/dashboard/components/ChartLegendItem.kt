package iz.mkao.mirasalon.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.designsystem.theme.*

@Composable
fun ChartLegendItem(color: Color, label: String, hasInnerDot: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(10.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            if (hasInnerDot) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = MiraTextPrimary, fontWeight = FontWeight.Medium)
    }
}
