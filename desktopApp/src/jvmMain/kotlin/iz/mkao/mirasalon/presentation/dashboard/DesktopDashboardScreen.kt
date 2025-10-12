package iz.mkao.mirasalon.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.domain.model.ActivityEvent
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStats
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.SalesTrend
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.LocalDesktopNavigate
import iz.mkao.mirasalon.presentation.LocalProfileClick
import iz.mkao.mirasalon.presentation.LocalSidebarExpanded
import iz.mkao.mirasalon.presentation.LocalToggleSidebar
import iz.mkao.mirasalon.presentation.components.DesktopLoadingState
import iz.mkao.mirasalon.presentation.dashboard.cards.AnalyticsCard
import iz.mkao.mirasalon.presentation.dashboard.cards.AppointmentActivityCard
import iz.mkao.mirasalon.presentation.dashboard.cards.RecentSalesCard
import iz.mkao.mirasalon.presentation.dashboard.cards.UpcomingAppointmentsCard
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardHeader
import iz.mkao.mirasalon.presentation.dashboard.components.Sidebar

@Composable
fun DesktopDashboardScreenUi(
    state: DesktopDashboardUiState,
    modifier: Modifier = Modifier
) {
    val isSidebarExpanded = LocalSidebarExpanded.current
    val onToggleSidebar = LocalToggleSidebar.current
    val onNavigate = LocalDesktopNavigate.current
    val onProfileClick = LocalProfileClick.current

    Row(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Sidebar(
            isExpanded = isSidebarExpanded,
            onToggle = onToggleSidebar,
            selectedRoute = "Dashboard",
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxHeight().width(if (isSidebarExpanded) 280.dp else 80.dp)
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF9F9F9))) {
            Box(modifier = Modifier.padding(horizontal = 40.dp, vertical = 24.dp)) {
                DashboardHeader(
                    title = "Welcome back",
                    userName = state.userName,
                    userAvatar = state.userAvatar,
                    onProfileClick = onProfileClick
                )
            }

            if (state.isLoading && state.salesTrend == null) {
                DesktopLoadingState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    DashboardContent(
                        salesTrend = state.salesTrend,
                        appointmentStats = state.appointmentStats,
                        recentActivity = state.recentActivity,
                        lowStockProducts = state.lowStockProducts
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    salesTrend: SalesTrend?,
    appointmentStats: AdminAppointmentStats?,
    recentActivity: List<ActivityEvent>,
    lowStockProducts: List<Product>
) {
    Column {
        Text(
            "Your overview",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
            RecentSalesCard(
                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                data = salesTrend,
                stats = appointmentStats
            )
            Spacer(modifier = Modifier.width(20.dp))
            UpcomingAppointmentsCard(
                modifier = Modifier.weight(0.8f).fillMaxHeight(),
                data = appointmentStats
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
            AnalyticsCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                products = lowStockProducts
            )
            Spacer(modifier = Modifier.width(20.dp))
            AppointmentActivityCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                activities = recentActivity
            )
        }
    }
}

/** Circuit [Ui.Factory] binding [DesktopScreen.Dashboard] to [DesktopDashboardScreenUi]. */
class DesktopDashboardUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Dashboard -> ui<DesktopDashboardUiState> { state, modifier ->
            DesktopDashboardScreenUi(
                state = state,
                modifier = modifier
            )
        }
        else -> null
    }
}
