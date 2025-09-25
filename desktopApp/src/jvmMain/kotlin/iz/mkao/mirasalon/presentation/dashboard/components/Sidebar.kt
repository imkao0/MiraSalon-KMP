package iz.mkao.mirasalon.presentation.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ViewSidebar
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.RadiusExtraSmall
import iz.mkao.mirasalon.core.designsystem.theme.RadiusLarge

@Composable
fun Sidebar(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    selectedRoute: String = "Dashboard",
    onNavigate: (String) -> Unit = {}
) {
    Surface(
        modifier = modifier.border(width = 1.dp, color = MiraBorder, shape = RectangleShape),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(horizontal = if (isExpanded) 24.dp else 12.dp, vertical = 24.dp)) {
            // Logo Area
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExpanded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCut,
                            contentDescription = "Logo",
                            tint = MiraCoral,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "MiraSalon",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = MiraTextPrimary,
                            fontSize = 18.sp
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ContentCut,
                        contentDescription = "Logo",
                        tint = MiraCoral,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isExpanded) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ViewSidebar,
                        contentDescription = "Collapse",
                        tint = MiraTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onToggle() }
                    )
                }
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ViewSidebar,
                    contentDescription = "Expand",
                    tint = MiraTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.CenterHorizontally)
                        .clickable { onToggle() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Search Bar
            if (isExpanded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(RadiusExtraSmall)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, null, tint = MiraTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search", color = MiraTextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("⌘K", color = MiraTextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Salon Section
            SidebarSection("Salon", isExpanded)
            SidebarItem(
                icon = Icons.Outlined.Home,
                label = "Dashboard",
                isExpanded = isExpanded,
                isSelected = selectedRoute == "Dashboard",
                onClick = { onNavigate("Dashboard") },
                hasBox = true
            )
            SidebarItem(Icons.Outlined.CalendarToday, "Calendar", isExpanded = isExpanded, isSelected = selectedRoute == "Calendar", onClick = { onNavigate("Calendar") })
            SidebarItem(Icons.Outlined.EventAvailable, "Bookings", isExpanded = isExpanded, isSelected = selectedRoute == "Bookings", onClick = { onNavigate("Bookings") })
            SidebarItem(Icons.Outlined.Face, "Staff", isExpanded = isExpanded, isSelected = selectedRoute == "Staff", onClick = { onNavigate("Staff") })
            SidebarItem(Icons.Outlined.Analytics, "Analytics", isExpanded = isExpanded, isSelected = selectedRoute == "Analytics", onClick = { onNavigate("Analytics") })
            SidebarItem(Icons.Outlined.AutoAwesome, "Promotions", isExpanded = isExpanded, isSelected = selectedRoute == "Promotions", onClick = { onNavigate("Promotions") })
            SidebarItem(Icons.Outlined.Grade, "Reviews", isExpanded = isExpanded, isSelected = selectedRoute == "Reviews", onClick = { onNavigate("Reviews") })
            SidebarItem(Icons.AutoMirrored.Outlined.Chat, "Chat", isExpanded = isExpanded, isSelected = selectedRoute == "Chat", onClick = { onNavigate("Chat") })

            Spacer(modifier = Modifier.height(if (isExpanded) 24.dp else 12.dp))

            // Services Section
            SidebarSection("Services", isExpanded)
            SidebarItem(Icons.Outlined.Face, "Customers", isExpanded = isExpanded, isSelected = selectedRoute == "Customers", onClick = { onNavigate("Customers") })
            SidebarItem(Icons.Outlined.ContentCut, "Services", isExpanded = isExpanded, isSelected = selectedRoute == "Services", onClick = { onNavigate("Services") })
            SidebarItem(Icons.Outlined.Inventory2, "Products", isExpanded = isExpanded, isSelected = selectedRoute == "Products", onClick = { onNavigate("Products") })
            SidebarItem(Icons.Outlined.ShoppingBag, "Orders & Deliveries", isExpanded = isExpanded, isSelected = selectedRoute == "Orders", onClick = { onNavigate("Orders") })

            Spacer(modifier = Modifier.weight(1f))

            if (isExpanded) {
                UpdatePromoCard()
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Items
            SidebarItem(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                label = "Help & Support",
                isExpanded = isExpanded,
                isSelected = selectedRoute == "Help",
                onClick = { onNavigate("Help") }
            )
            SidebarItem(Icons.AutoMirrored.Outlined.ExitToApp, "Sign out", isExpanded = isExpanded, onClick = { onNavigate("Sign out") })
            SidebarItem(Icons.Outlined.Settings, "Settings", isExpanded = isExpanded, isSelected = selectedRoute == "Settings", onClick = { onNavigate("Settings") })
        }
    }
}

@Composable
fun UpdatePromoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        color = Color.White,
        shape = RoundedCornerShape(RadiusLarge),
        border = BorderStroke(1.dp, MiraBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Background image or gradient
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF06A6A), Color(0xFFFFB74D))
                        )
                    )
                )
                // Dashboard illustration placeholder
                Icon(
                    Icons.Outlined.Dashboard,
                    null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(60.dp).align(Alignment.Center)
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("We've made some updates", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MiraTextPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Outlined.NorthEast, null, tint = MiraTextSecondary, modifier = Modifier.size(10.dp))
                }
                Text("See what's new", fontSize = 11.sp, color = MiraTextSecondary)
            }
        }
    }
}

@Composable
fun SidebarSection(title: String, isExpanded: Boolean) {
    if (isExpanded) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.DarkGray,
            modifier = Modifier.padding(vertical = 12.dp),
            fontSize = 13.sp
        )
    }
}

@Composable
fun SidebarItem(
    icon: ImageVector,
    label: String,
    isExpanded: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    hasBox: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .padding(horizontal = if (isExpanded) 4.dp else 0.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
    ) {
        if (hasBox && isSelected) {
            Surface(
                modifier = Modifier.size(24.dp),
                color = MiraCoral.copy(alpha = 0.1f),
                shape = RoundedCornerShape(RadiusExtraSmall),
                border = BorderStroke(1.dp, MiraCoral)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = MiraCoral,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MiraCoral else MiraTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        if (isExpanded) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MiraCoral else MiraTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
