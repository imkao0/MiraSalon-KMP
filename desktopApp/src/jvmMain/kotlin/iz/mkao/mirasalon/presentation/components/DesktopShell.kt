package iz.mkao.mirasalon.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.data.local.TokenManager
import iz.mkao.mirasalon.presentation.LocalDesktopNavigate
import iz.mkao.mirasalon.presentation.LocalProfileClick
import iz.mkao.mirasalon.presentation.LocalSidebarExpanded
import iz.mkao.mirasalon.presentation.LocalToggleSidebar
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardHeader
import iz.mkao.mirasalon.presentation.dashboard.components.Sidebar
import org.koin.compose.koinInject

@Composable
fun DesktopShell(
    title: String,
    subtitle: String? = null,
    selectedRoute: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokenManager: TokenManager = koinInject()
    val session by tokenManager.session.collectAsState()
    
    val isSidebarExpanded = LocalSidebarExpanded.current
    val onToggleSidebar = LocalToggleSidebar.current
    val onNavigate = LocalDesktopNavigate.current
    val onProfileClick = LocalProfileClick.current

    Row(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Sidebar(
            isExpanded = isSidebarExpanded,
            onToggle = onToggleSidebar,
            modifier = Modifier
                .width(if (isSidebarExpanded) 260.dp else 80.dp)
                .fillMaxHeight()
                .animateContentSize(),
            selectedRoute = selectedRoute,
            onNavigate = onNavigate
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, end = 40.dp, top = 16.dp)
        ) {
            DashboardHeader(
                title = title,
                subtitle = subtitle,
                userName = session.name,
                userAvatar = session.avatarUrl,
                onProfileClick = onProfileClick
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}
