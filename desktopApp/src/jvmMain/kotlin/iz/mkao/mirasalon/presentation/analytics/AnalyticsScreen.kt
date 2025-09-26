package iz.mkao.mirasalon.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.MiraYellow
import iz.mkao.mirasalon.core.designsystem.theme.RadiusMedium
import iz.mkao.mirasalon.core.designsystem.theme.VelvetaPistachioSoft
import iz.mkao.mirasalon.core.designsystem.theme.VelvetaSlateBlue
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.components.DesktopLoadingState
import iz.mkao.mirasalon.presentation.components.DesktopShell
import iz.mkao.mirasalon.presentation.dashboard.DesktopDashboardEvent
import iz.mkao.mirasalon.presentation.dashboard.DesktopDashboardUiState
import iz.mkao.mirasalon.presentation.dashboard.charts.AppointmentBarData
import iz.mkao.mirasalon.presentation.dashboard.charts.SalesLineChart
import iz.mkao.mirasalon.presentation.dashboard.charts.StackedBarChart
import iz.mkao.mirasalon.presentation.dashboard.components.ChartLegendItem
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardCard

@Composable
fun AnalyticsScreenUi(
    state: DesktopDashboardUiState,
    modifier: Modifier = Modifier
) {
    val salesTrend = state.salesTrend
    val appointmentStats = state.appointmentStats
    val specialistPerformance = state.specialistPerformance
    val servicePopularity = state.servicePopularity
    val selectedDays = state.selectedDays

    DesktopShell(
        title = "Business Analytics",
        subtitle = "Deep dive into performance",
        selectedRoute = "Analytics"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var showTimeRangeMenu by remember { mutableStateOf(false) }

            Box {
                Surface(
                    onClick = { showTimeRangeMenu = true },
                    modifier = Modifier.background(Color.White, RoundedCornerShape(RadiusMedium)).border(1.dp, MiraBorder, RoundedCornerShape(RadiusMedium)),
                    color = Color.White,
                    shape = RoundedCornerShape(RadiusMedium)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$selectedDays Days",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiraTextPrimary
                        )
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            tint = MiraTextPrimary
                        )
                    }
                }

                DropdownMenu(
                    expanded = showTimeRangeMenu,
                    onDismissRequest = { showTimeRangeMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("7 Days") },
                        onClick = {
                            state.eventSink(DesktopDashboardEvent.TimeRangeChanged(7))
                            showTimeRangeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("30 Days") },
                        onClick = {
                            state.eventSink(DesktopDashboardEvent.TimeRangeChanged(30))
                            showTimeRangeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("90 Days") },
                        onClick = {
                            state.eventSink(DesktopDashboardEvent.TimeRangeChanged(90))
                            showTimeRangeMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading && salesTrend == null) {
            DesktopLoadingState()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    DashboardCard(
                        title = "Booking Trends",
                        subtitle = "Daily appointment volume",
                        timeLabel = "$selectedDays days"
                    ) {
                        val appointments = salesTrend?.points?.map { it.appointments.toFloat() } ?: emptyList()
                        val labels = salesTrend?.points?.map { it.date } ?: emptyList()

                        if (appointments.isNotEmpty()) {
                            SalesLineChart(
                                primaryData = appointments,
                                dayLabels = labels,
                                dayStartIndices = List(labels.size) { it },
                                modifier = Modifier.fillMaxWidth().height(300.dp),
                                maxValue = (appointments.maxOrNull() ?: 15f).coerceAtLeast(15f),
                                yAxisLabel = "Bookings",
                                primaryColor = VelvetaSlateBlue,
                                isCurrency = false
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                Text("No data available", color = MiraTextSecondary)
                            }
                        }
                    }
                }

                item {
                    DashboardCard(
                        title = "Client Acquisition",
                        subtitle = "New vs Returning customers",
                        timeLabel = "$selectedDays days"
                    ) {
                        val data = appointmentStats?.points?.map {
                            AppointmentBarData(
                                it.date,
                                it.returningClients.toFloat(),
                                it.newClients.toFloat()
                            )
                        } ?: emptyList()

                        if (data.isNotEmpty()) {
                            StackedBarChart(
                                data = data,
                                modifier = Modifier.fillMaxWidth().height(300.dp),
                                maxValue = (data.maxOfOrNull { it.confirmed + it.cancelled } ?: 15f).coerceAtLeast(15f),
                                yAxisLabel = "Customers",
                                primaryColor = MiraYellow,
                                secondaryColor = MiraCoral
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ChartLegendItem(MiraYellow, "Returning", hasInnerDot = true)
                                Spacer(modifier = Modifier.width(16.dp))
                                ChartLegendItem(MiraCoral, "New", hasInnerDot = true)
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                Text("No data available", color = MiraTextSecondary)
                            }
                        }
                    }
                }

                item {
                    DashboardCard(
                        title = "Specialist Performance",
                        subtitle = "Target achievement",
                        timeLabel = "$selectedDays days"
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                        ) {
                            if (specialistPerformance.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                    Text("No performance data available", color = MiraTextSecondary)
                                }
                            } else {
                                specialistPerformance.forEach { specialist ->
                                    PerformanceItem(
                                        specialist.name,
                                        specialist.targetAchievement,
                                        when {
                                            specialist.targetAchievement >= 0.9f -> VelvetaPistachioSoft
                                            specialist.targetAchievement >= 0.7f -> VelvetaSlateBlue
                                            else -> MiraCoral
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    DashboardCard(
                        title = "Service Popularity",
                        subtitle = "Most booked services",
                        timeLabel = "$selectedDays days"
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                        ) {
                            if (servicePopularity.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                    Text("No service data available", color = MiraTextSecondary)
                                }
                            } else {
                                servicePopularity.forEach { service ->
                                    PopularService(
                                        service.name,
                                        service.count,
                                        service.ratio,
                                        selectedDays
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceItem(name: String, progress: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MiraTextPrimary)
            Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = MiraTextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.White, RoundedCornerShape(2.dp)).border(0.5.dp, MiraBorder, RoundedCornerShape(2.dp)),
            color = color,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun PopularService(name: String, count: Int, ratio: Float, days: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MiraTextPrimary)
            Text("$count bookings last $days days", fontSize = 12.sp, color = MiraTextSecondary)
        }
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(24.dp)
                .background(Color.White, RoundedCornerShape(2.dp))
                .border(0.5.dp, MiraBorder, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio)
                    .background(MiraCoral, RoundedCornerShape(2.dp))
            )
        }
    }
}

/** Circuit [Ui.Factory] binding [DesktopScreen.Analytics] to [AnalyticsScreenUi]. */
class AnalyticsUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Analytics -> ui<DesktopDashboardUiState> { state, modifier ->
            AnalyticsScreenUi(
                state = state,
                modifier = modifier
            )
        }
        else -> null
    }
}
