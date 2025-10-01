package iz.mkao.mirasalon.presentation.dashboard.cards

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
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
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraGreen
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.MiraYellow
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStats
import iz.mkao.mirasalon.core.domain.model.SalesTrend
import iz.mkao.mirasalon.presentation.dashboard.charts.SalesLineChart
import iz.mkao.mirasalon.presentation.dashboard.components.ChartLegendItem
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardCard
import kotlin.math.abs

@Composable
fun RecentSalesCard(
    modifier: Modifier = Modifier,
    data: SalesTrend? = null,
    stats: AdminAppointmentStats? = null,
    selectedDays: Int = 7
) {
    val sales = data?.points?.map { it.amount.toFloat() } ?: emptyList()
    val appointments = data?.points?.map { it.appointments.toFloat() } ?: emptyList()
    val dayLabels = data?.points?.map { it.date.takeLast(5) } ?: emptyList()
    val dayStartIndices = sales.indices.toList()

    val totalSales = data?.points?.sumOf { it.amount } ?: 0.0

    DashboardCard(
        modifier = modifier,
        title = "Recent sales",
        subtitle = "Track your latest sales",
        topRightContent = {
            Surface(shape = RoundedCornerShape(2.dp), color = Color.White) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).border(1.dp, MiraBorder, RoundedCornerShape(2.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MiraTextSecondary, modifier = Modifier.size(14.dp))
                    Text("$selectedDays days", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MiraTextSecondary)
                }
            }
        }
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val salesDisplay = stats?.revenue ?: totalSales
                Text("$${"%,.2f".format(salesDisplay)}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.width(12.dp))
                
                val growth = stats?.revenueGrowth ?: data?.revenueGrowth ?: 0.0
                val isPositive = growth >= 0
                val growthColor = if (isPositive) MiraGreen else MiraCoral
                val growthIcon = if (isPositive) "↗" else "↘"

                Surface(
                    color = growthColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(growthIcon, color = growthColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${"%.1f".format(abs(growth))}%", color = growthColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Appointments ", fontSize = 13.sp, color = MiraTextSecondary)
                    Text("${stats?.confirmed ?: data?.points?.sumOf { it.appointments } ?: 0}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Appts value ", fontSize = 13.sp, color = MiraTextSecondary)
                    Text("$${"%,.2f".format(stats?.appointmentRevenue ?: data?.appointmentRevenue ?: 0.0)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sales value ", fontSize = 13.sp, color = MiraTextSecondary)
                    Text("$${"%,.2f".format(stats?.productRevenue ?: data?.productRevenue ?: 0.0)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (sales.isNotEmpty()) {
                val maxSales = sales.maxOrNull() ?: 1f
                val maxAppts = appointments.maxOrNull() ?: 1f


                val scaleFactor = if (maxAppts > 0) (maxSales * 0.7f) / maxAppts else 1f
                val scaledAppointments = appointments.map { it * scaleFactor }

                SalesLineChart(
                    primaryData = sales,
                    secondaryData = scaledAppointments,
                    dayLabels = dayLabels,
                    dayStartIndices = dayStartIndices,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxValue = maxSales.coerceAtLeast(60f),
                    yAxisLabel = "Total revenue",
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
                ChartLegendItem(MiraYellow, "Sales", hasInnerDot = true)
                Spacer(modifier = Modifier.width(16.dp))
                ChartLegendItem(MiraCoral, "Appointments", hasInnerDot = true)
            }
        }
    }
}
