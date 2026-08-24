package iz.mkao.mirasalon.feature.chat.presentation.circuit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.designsystem.components.MiraEmptyState
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.ChatRoute
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.feature.chat.domain.model.ChatItem
import iz.mkao.mirasalon.feature.chat.domain.model.DeliveryStatus
import iz.mkao.mirasalon.feature.chat.domain.model.QuickAccessContact

@Composable
fun ChatListContent(
    state: ChatListState,
    modifier: Modifier = Modifier
) {
    when (state) {
        is ChatListState.Loading -> ChatsLoadingUi(modifier)
        is ChatListState.Error   -> ChatsErrorUi(state, modifier)
        is ChatListState.Content -> ChatsContentUi(state, modifier)
    }
}

class ChatManualUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is ChatRoute.ChatList -> ui<ChatListState> { state, modifier -> ChatListContent(state, modifier) }
            is BottomNavKey.Chat -> ui<ChatListState> { state, modifier -> ChatListContent(state, modifier) }
            is ChatRoute.ChatDetail -> ui<ChatDetailState> { state, modifier -> ChatDetailContent(state, modifier) }
            else -> null
        }
    }
}

@Composable
private fun ChatsLoadingUi(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ShimmerLoading()
    }
}

@Composable
private fun ChatsErrorUi(state: ChatListState.Error, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsContentUi(
    state: ChatListState.Content,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MiraTopAppBar(
                title = "Chats",
                actions = {
                    val resolvedUrl = ApiEndpoints.resolveImageUrl(state.currentUserAvatarUrl)
                    if (resolvedUrl != null) {
                        AsyncImage(
                            model = resolvedUrl,
                            contentDescription = "Your profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable(onClick = { state.eventSink(ChatListEvent.OpenProfile) })
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    } else {
                        IconButton(
                            onClick = { state.eventSink(ChatListEvent.OpenProfile) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Your profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    var showMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete History") },
                                onClick = {
                                    showMenu = false
                                    state.eventSink(ChatListEvent.DeleteHistory)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                QuickAccessRow(
                    contacts = state.quickAccessContacts,
                    onSearchClick = { state.eventSink(ChatListEvent.OpenSearch()) },
                    onContactClick = { state.eventSink(ChatListEvent.OpenQuickContact(it)) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.chats.isEmpty()) {
                item {
                    MiraEmptyState(
                        message = "No messages yet",
                        description = "Start a conversation with our specialists to get professional advice and personalized care.",
                        icon = Icons.Outlined.ChatBubbleOutline,
                        modifier = Modifier.fillParentMaxSize()
                    )
                }
            } else {
                items(items = state.chats.distinctBy { it.id }, key = { it.id }) { chat ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                state.eventSink(ChatListEvent.DeleteChat(chat.id))
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = { DismissBackground(dismissState) }
                    ) {
                        ChatRow(
                            item = chat,
                            onClick = { state.eventSink(ChatListEvent.OpenChat(chat.id)) },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 80.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }
    val alignment = Alignment.CenterEnd
    val icon = Icons.Outlined.Delete

    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
            Icon(
                icon,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun QuickAccessRow(
    contacts: List<QuickAccessContact>,
    onSearchClick: () -> Unit,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier            = modifier.fillMaxWidth(),
        contentPadding      = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { QuickAccessSearchItem(onClick = onSearchClick) }
        items(contacts.distinctBy { it.id }, key = { it.id }) { contact ->
            QuickAccessContactItem(
                contact = contact,
                onClick = { onContactClick(contact.id) },
            )
        }
    }
}

@Composable
private fun QuickAccessSearchItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier              = modifier.clickable(onClick = onClick),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier           = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment   = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Outlined.Search,
                contentDescription = "Search",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(28.dp),
            )
        }
        Text(
            text  = "Search",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuickAccessContactItem(
    contact: QuickAccessContact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val resolvedUrl = ApiEndpoints.resolveImageUrl(contact.avatarUrl)
        AsyncImage(
            model              = resolvedUrl,
            contentDescription = contact.name,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(
            text     = contact.name,
            style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChatRow(
    item: ChatItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(52.dp)) {
            val resolvedUrl = ApiEndpoints.resolveImageUrl(item.avatarUrl)
            AsyncImage(
                model              = resolvedUrl,
                contentDescription = item.contactName,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            if (item.isOnline) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .align(Alignment.BottomEnd),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = item.contactName,
                style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.contactRole?.let { role ->
                Text(
                    text     = role,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text     = item.lastMessage,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = item.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = if (item.unreadCount > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (item.unreadCount > 0) {
                Text(
                    text = item.unreadCount.toString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            } else {
                DeliveryStatusIcon(status = item.deliveryStatus)
            }
        }
    }
}

@Composable
private fun DeliveryStatusIcon(
    status: DeliveryStatus,
    modifier: Modifier = Modifier,
) {
    val tint = when (status) {
        DeliveryStatus.Read                           -> Color(0xFF4CAF50)
        DeliveryStatus.Delivered, DeliveryStatus.Sent -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Icon(
        imageVector        = Icons.Outlined.Check,
        contentDescription = status.name,
        tint               = tint,
        modifier           = modifier.size(16.dp),
    )
}
