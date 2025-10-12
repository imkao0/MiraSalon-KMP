package iz.mkao.mirasalon.presentation.dashboard.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.designsystem.theme.*
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStats
import iz.mkao.mirasalon.presentation.dashboard.charts.AppointmentBarData
import iz.mkao.mirasalon.presentation.dashboard.charts.StackedBarChart
import iz.mkao.mirasalon.presentation.dashboard.components.ChartLegendItem
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardCard

@Composable
fun UpcomingAppointmentsCard(
    modifier: Modifier = Modifier,
    data: AdminAppointmentStats? = null
) {
    var is7DaysClicked by remember { mutableStateOf(false) }

    DashboardCard(
        modifier = modifier,
        title = "Upcoming appointments",
        subtitle = "Track for bookings",
        topRightContent = {
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = if (is7DaysClicked) Color.LightGray else VelvetaOffWhiteLight,
                modifier = Modifier.clickable { is7DaysClicked = !is7DaysClicked }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MiraTextSecondary, modifier = Modifier.size(14.dp))
                    Text("7 days", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MiraTextSecondary)
                }
            }
        }
    ) {
        Column {
            Text(
                "${data?.total ?: 0}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row {
                    Text("Confirmed ", fontSize = 13.sp, color = MiraTextSecondary)
                    Text("${data?.confirmed ?: 0}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                VerticalDivider(modifier = Modifier.height(12.dp), color = MiraBorder, thickness = 1.dp)
                Row {
                    Text("Cancelled ", fontSize = 13.sp, color = MiraTextSecondary)
                    Text("${data?.cancelled ?: 0}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val barData = data?.points?.map {
                AppointmentBarData(it.date.takeLast(5), it.confirmed.toFloat(), it.cancelled.toFloat())
            } ?: emptyList()

            if (barData.isNotEmpty()) {
                StackedBarChart(
                    data = barData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxValue = barData.maxOf { it.confirmed + it.cancelled }.coerceAtLeast(15f),
                    yAxisLabel = "Number of appointment",
                    primaryColor = MiraYellow,
                    secondaryColor = MiraCoral
                )
            } else {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No data available", color = MiraTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ChartLegendItem(MiraYellow, "Confirmed", hasInnerDot = true)
                Spacer(modifier = Modifier.width(16.dp))
                ChartLegendItem(MiraCoral, "Cancelled", hasInnerDot = true)
            }
        }
    }
}
