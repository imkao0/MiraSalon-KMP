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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.domain.model.Notification
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.data.local.TokenManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun DashboardHeader(
    title: String = "Welcome back",
    subtitle: String? = null,
    userName: String? = null,
    userAvatar: String? = null,
    onProfileClick: () -> Unit = {}
) {
    val tokenManager: TokenManager = koinInject()
    val notificationRepository: NotificationRepository = koinInject()
    
    val session by tokenManager.session.collectAsState()
    val notifications by notificationRepository.notifications.collectAsState(initial = emptyList())
    
    val scope = rememberCoroutineScope()
    var showNotifications by remember { mutableStateOf(false) }
    
    val effectiveAvatar = userAvatar ?: session.avatarUrl

    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                color = MiraTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.clickable {  }
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.width(32.dp))
                Text(subtitle, color = MiraTextSecondary, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(32.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {  }) {
                Text("Campaign assistant", color = MiraTextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Demo",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color(0xFF1976D2),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(32.dp))
            Text(
                "Trend predictions",
                color = MiraTextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.clickable {  }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = MiraTextSecondary, modifier = Modifier.size(20.dp).clickable {  })
            Spacer(modifier = Modifier.width(24.dp))
            
            Box {
                BadgedBox(
                    badge = {
                        if (notifications.isNotEmpty()) {
                            Badge(
                                containerColor = MiraCoral,
                                contentColor = Color.White
                            ) {
                                Text(notifications.size.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        null,
                        tint = MiraTextSecondary,
                        modifier = Modifier.size(20.dp).clickable { showNotifications = true }
                    )
                }

                DropdownMenu(
                    expanded = showNotifications,
                    onDismissRequest = { showNotifications = false },
                    modifier = Modifier
                        .width(420.dp)
                        .background(Color.White)
                        .border(1.dp, MiraBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Notifications",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MiraCoral.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "${notifications.size} New",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MiraCoral,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            IconButton(onClick = { showNotifications = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = MiraTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("All updates", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = MiraTextSecondary, modifier = Modifier.size(16.dp))
                            }
                            
                            Text(
                                "Mark all as read",
                                color = MiraTextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { 
                                    scope.launch { notificationRepository.clearNotifications() }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MiraBorder.copy(alpha = 0.5f))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (notifications.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No new notifications",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MiraTextSecondary
                                    )
                                }
                            } else {
                                notifications.forEachIndexed { index, notification ->
                                    NotificationItem(notification)
                                    if (index < notifications.size - 1) {
                                        HorizontalDivider(color = MiraBorder.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedButton(
                            onClick = { showNotifications = false },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MiraBorder)
                        ) {
                            Text("View all notifications", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))
            Icon(Icons.Outlined.ChatBubbleOutline, null, tint = MiraTextSecondary, modifier = Modifier.size(20.dp).clickable {  })
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                if (!effectiveAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = effectiveAvatar,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = {
                            Napier.e(it.result.throwable) { "Failed to load profile image" }
                        }
                    )
                } else {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MiraTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: Notification) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MiraBorder.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(notification.senderName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${notification.senderName} ${notification.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Text(
                    "Just now",
                    style = MaterialTheme.typography.labelSmall,
                    color = MiraTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MiraCoral))
        }
    }
}
