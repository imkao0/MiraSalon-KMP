package iz.mkao.mirasalon.feature.notifications.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.designsystem.components.MiraCenterAlignedTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.MiraEmptyState
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraGreen
import iz.mkao.mirasalon.core.designsystem.theme.ProfileAvatarSize
import iz.mkao.mirasalon.core.designsystem.theme.SpacingLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall
import iz.mkao.mirasalon.core.designsystem.theme.SpacingTiny
import iz.mkao.mirasalon.core.designsystem.theme.StrokeThin
import iz.mkao.mirasalon.core.domain.model.NotificationType
import iz.mkao.mirasalon.core.navigation.NotificationRoute
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.feature.notifications.presentation.circuit.NotificationEvent
import iz.mkao.mirasalon.feature.notifications.presentation.circuit.NotificationItem
import iz.mkao.mirasalon.feature.notifications.presentation.circuit.NotificationState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    state: NotificationState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            MiraCenterAlignedTopAppBar(
                title = "Notifications",
                onBackClick = { state.eventSink(NotificationEvent.BackClicked) },
                actions = {
                    var showFilterMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Filter")
                    }

                    IconButton(onClick = { state.eventSink(NotificationEvent.ClearAll) }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear All")
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                state.eventSink(NotificationEvent.FilterChanged(null))
                                showFilterMenu = false
                            }
                        )
                        NotificationType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    state.eventSink(NotificationEvent.FilterChanged(type))
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.notifications.isEmpty()) {
                MiraEmptyState(
                    message = "No notifications yet",
                    description = "We'll notify you when something important happens, like booking updates or special offers.",
                    icon = Icons.Outlined.Notifications
                )
            } else {
                val tz = remember { TimeZone.currentSystemDefault() }
                val now = remember(state.currentTimeMillis) { 
                    kotlin.time.Instant.fromEpochMilliseconds(state.currentTimeMillis).toLocalDateTime(tz).date
                }
                
                val todayNotifications = state.notifications.filter { 
                    kotlin.time.Instant.fromEpochMilliseconds(it.timestamp).toLocalDateTime(tz).date == now
                }
                val earlierNotifications = state.notifications.filter { it !in todayNotifications }
                
                val todayCount = todayNotifications.size
                Text(
                    text = "You have $todayCount Notifications today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = SpacingLarge, vertical = SpacingTiny)
                )

                Spacer(modifier = Modifier.height(SpacingLarge))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = SpacingLarge)
                ) {
                    if (todayNotifications.isNotEmpty()) {
                        item {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = SpacingLarge, vertical = SpacingLarge)
                            )
                        }

                        items(todayNotifications) { item ->
                            NotificationRow(
                                item = item,
                                onClick = { state.eventSink(NotificationEvent.NotificationClicked(item.id)) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = SpacingLarge),
                                thickness = StrokeThin,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }

                    if (earlierNotifications.isNotEmpty()) {
                        item {
                            Text(
                                text = "This Week",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = SpacingLarge, vertical = SpacingLarge)
                            )
                        }

                        items(earlierNotifications) { item ->
                            NotificationRow(
                                item = item,
                                onClick = { state.eventSink(NotificationEvent.NotificationClicked(item.id)) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = SpacingLarge),
                                thickness = StrokeThin,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationRow(
    item: NotificationItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = SpacingMedium, vertical = SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(modifier = Modifier.width(SpacingLarge)) {
            if (item.isUnread) {
                Box(
                    modifier = Modifier
                        .size(SpacingSmall)
                        .clip(CircleShape)
                        .background(MiraGreen)
                        .align(Alignment.Center)
                )
            }
        }


        Box(modifier = Modifier.size(ProfileAvatarSize)) {
            if (item.type == NotificationType.PROMO && item.senderAvatarUrl == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        modifier = Modifier.size(SpacingLarge),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                AsyncImage(
                    model = ApiEndpoints.resolveImageUrl(item.senderAvatarUrl) ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=${item.senderName}",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            val icon = when (item.type) {
                NotificationType.COMMENT -> Icons.Outlined.ChatBubbleOutline
                NotificationType.PROMO -> Icons.Outlined.Email
                NotificationType.MESSAGE -> Icons.AutoMirrored.Outlined.Message
                NotificationType.REMINDER -> Icons.Outlined.Notifications
            }
            
            val iconColor = when (item.type) {
                NotificationType.PROMO -> MiraCoral
                NotificationType.REMINDER -> Color(0xFFFFA000)
                else -> MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .shadow(elevation = 1.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = iconColor
                )
            }
        }

        Spacer(modifier = Modifier.width(SpacingMedium))


        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = item.senderName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(SpacingTiny))
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = item.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.type == NotificationType.REMINDER && item.reminderTimeDetails != null) {
                Text(
                    text = "Reminder: ${item.reminderTimeDetails}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

class NotificationUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is NotificationRoute.Notifications -> ui<NotificationState> { state, _ -> NotificationScreen(state) }
            else -> null
        }
    }
}
