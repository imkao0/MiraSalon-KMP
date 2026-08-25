package iz.mkao.mirasalon.presentation.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.ChatBubbleReceived
import iz.mkao.mirasalon.core.designsystem.theme.ChatBubbleSent
import iz.mkao.mirasalon.core.designsystem.theme.ChatTextReceived
import iz.mkao.mirasalon.core.designsystem.theme.ChatTextSent
import iz.mkao.mirasalon.core.designsystem.theme.ChatTimeText
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.RadiusSmall
import iz.mkao.mirasalon.core.designsystem.theme.Success
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.model.chat.ChatMessage
import iz.mkao.mirasalon.core.domain.model.chat.ChatSession
import iz.mkao.mirasalon.core.domain.model.chat.MessageContent
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.components.DesktopShell
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.skia.Image
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.time.Clock

@Composable
fun AdminChatScreenUi(
    state: AdminChatUiState,
    modifier: Modifier = Modifier
) {
    DesktopShell(
        title = "Support",
        subtitle = "Real-time client support",
        selectedRoute = "Chat"
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusSmall),
            color = Color.White,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, MiraBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Staff Members",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.specialists) { specialist ->
                        SpecialistChatAvatar(
                            specialist = specialist,
                            isSelected = state.selectedSpecialistId == specialist.id,
                            unreadCount = state.specialistUnreadCounts[specialist.id] ?: 0,
                            onClick = { state.eventSink(AdminChatEvent.SelectSpecialist(specialist.id)) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.weight(1f)) {
            Surface(
                modifier = Modifier.width(300.dp).fillMaxHeight(),
                shape = RectangleShape,
                color = Color.White,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, MiraBorder)
            ) {
                Column {
                    Text(
                        "Customers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )

                    HorizontalDivider(color = MiraBorder)

                    if (state.filteredChannels.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No active chats", color = MiraTextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.filteredChannels) { session ->
                                CustomerProfileItem(
                                    session = session,
                                    isSelected = state.selectedSession?.id == session.id,
                                    onClick = { state.eventSink(AdminChatEvent.SelectSession(session.id)) }
                                )
                                HorizontalDivider(color = MiraBorder.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(RadiusSmall),
                color = Color.White,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, MiraBorder)
            ) {
                state.selectedSession?.let { session ->
                    val specialist = state.specialists.find { it.id == state.selectedSpecialistId }
                    DesktopAdminChatView(
                        session = session,
                        messages = state.messages,
                        inputText = state.inputText,
                        onInputTextChanged = { state.eventSink(AdminChatEvent.InputTextChanged(it)) },
                        onSendMessage = { state.eventSink(AdminChatEvent.SendMessage) },
                        specialistId = specialist?.id ?: state.selectedSpecialistId,
                        specialistName = specialist?.name ?: "Admin",
                        specialistAvatar = specialist?.imageUrl,
                        pendingImagePreview = state.pendingImagePreview,
                        isSendingImage = state.isSendingImage,
                        onPickImage = { bytes, name, preview ->
                            state.eventSink(AdminChatEvent.ImageSelected(bytes, name, preview))
                        },
                        onClearPendingImage = { state.eventSink(AdminChatEvent.ClearPendingImage) },
                        currentUserId = state.currentUserId
                    )
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Chat,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Select a customer to view conversation",
                                color = MiraTextSecondary,
                                style = MaterialTheme.typography.bodyLarge
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecialistChatAvatar(
    specialist: Specialist,
    isSelected: Boolean,
    unreadCount: Int = 0,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(72.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (isSelected) MiraCoral.copy(alpha = 0.12f) else Color.White)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MiraCoral else Color.LightGray.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .padding(if (isSelected) 5.dp else 0.dp)
            ) {
                val imageUrl = specialist.imageUrl
                if (!imageUrl.isNullOrBlank()) {
                    val fullUrl = ApiEndpoints.resolveImageUrl(imageUrl)
                    AsyncImage(
                        model = fullUrl,
                        contentDescription = specialist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        onError = {
                            Napier.e(it.result.throwable) { "Coil failed to load chat specialist image: $fullUrl" }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MiraCoral.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = specialist.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MiraCoral,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MiraCoral)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = specialist.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MiraCoral else Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = specialist.role,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = if (isSelected) MiraCoral.copy(alpha = 0.8f) else MiraTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CustomerProfileItem(
    session: ChatSession,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) MiraCoral.copy(alpha = 0.05f) else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            val avatarUrl = session.participantAvatarUrl
            if (!avatarUrl.isNullOrBlank()) {
                val fullUrl = ApiEndpoints.resolveImageUrl(avatarUrl)
                AsyncImage(
                    model = fullUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onError = {
                        Napier.e(it.result.throwable) { "Coil failed to load chat customer image: $fullUrl" }
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = session.participantName.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MiraTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (session.isActive) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.participantName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (session.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp)
                    .clip(RectangleShape)
                    .background(MiraCoral),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = session.unreadCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DesktopAdminChatView(
    session: ChatSession,
    messages: List<ChatMessage>,
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    specialistId: String?,
    specialistName: String?,
    specialistAvatar: String?,
    pendingImagePreview: ImageBitmap?,
    isSendingImage: Boolean,
    onPickImage: (bytes: ByteArray, fileName: String, preview: ImageBitmap?) -> Unit,
    onClearPendingImage: () -> Unit,
    currentUserId: String? = null
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                session.participantName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (session.isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                if (session.isActive) "Online" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = MiraTextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        HorizontalDivider(color = MiraBorder)


        val groupedMessages = messages.groupBy {
            val systemTZ = TimeZone.currentSystemDefault()
            Instant.fromEpochMilliseconds(it.timestamp).toLocalDateTime(systemTZ).date
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            groupedMessages.forEach { (date, messagesForDate) ->
                item(key = "date_$date") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 0.5.dp,
                            color = MiraBorder
                        )
                        val now = Clock.System.todayIn(TimeZone.currentSystemDefault())
                        val dateText = when (date) {
                            now -> "Today"
                            now.minus(1, DateTimeUnit.DAY) -> "Yesterday"
                            else -> {
                                val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
                                "$month ${date.dayOfMonth}, ${date.year}"
                            }
                        }
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MiraTextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 0.5.dp,
                            color = MiraBorder
                        )
                    }
                }
                items(messagesForDate) { message ->
                    val isMe = message.isFromAdmin || message.senderId == currentUserId || (specialistId != null && message.actingAsId == specialistId)
                    DesktopMessageBubble(
                        message = message,
                        isMe = isMe,
                        specialistName = if (isMe) specialistName else session.participantName
                    )
                }
            }
        }

        HorizontalDivider(color = MiraBorder)


        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            pendingImagePreview?.let { preview ->
                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(112.dp)
                        .border(1.dp, MiraBorder, RoundedCornerShape(RadiusSmall))
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = preview,
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(RadiusSmall)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onClearPendingImage,
                        modifier = Modifier.align(Alignment.TopEnd).size(30.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove image", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { pickChatImage(onPickImage) },
                    enabled = !isSendingImage
                ) {
                    Icon(
                        Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Attach image",
                        tint = if (isSendingImage) Color.LightGray else MiraCoral
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (pendingImagePreview == null) "Type a message..." else "Add a caption...") },
                    shape = RoundedCornerShape(RadiusSmall),
                    enabled = !isSendingImage,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiraCoral,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (isSendingImage) {
                    ShimmerLoading(modifier = Modifier.size(24.dp))
                } else {
                    val canSend = inputText.isNotBlank() || pendingImagePreview != null
                    IconButton(onClick = onSendMessage, enabled = canSend) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Send message",
                            tint = if (canSend) MiraCoral else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}


private fun pickChatImage(
    onPicked: (bytes: ByteArray, fileName: String, preview: ImageBitmap?) -> Unit
) {
    val chooser = JFileChooser().apply {
        dialogTitle = "Choose an image"
        fileFilter = FileNameExtensionFilter("Images (JPG, PNG, WEBP)", "jpg", "jpeg", "png", "webp")
        isAcceptAllFileFilterUsed = false
    }
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return

    runCatching {
        val file = chooser.selectedFile
        val bytes = file.readBytes()
        require(bytes.size <= 10 * 1024 * 1024) { "Image must be 10 MB or smaller" }
        val preview = Image.makeFromEncoded(bytes).toComposeImageBitmap()
        onPicked(bytes, file.name, preview)
    }.onFailure { error ->
        Napier.e(error) { "Unable to select chat image" }
    }
}

@Composable
fun DesktopMessageBubble(message: ChatMessage, isMe: Boolean, specialistName: String? = null) {
    val backgroundColor = when {
        message.isInternal -> Color(0xFFFFF9C4) // Light yellow for internal notes
        isMe -> ChatBubbleSent
        else -> ChatBubbleReceived
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (message.isInternal) {
            Text(
                "INTERNAL NOTE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {
            if (!isMe) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = specialistName?.take(1)?.uppercase() ?: "?",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiraTextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                color = backgroundColor,
                shape = RoundedCornerShape(RadiusSmall)
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    when (val content = message.content) {
                        is MessageContent.Text -> {
                            Text(
                                text = content.text,
                                color = if (message.isInternal) Color.Black else if (isMe) ChatTextSent else ChatTextReceived,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is MessageContent.Image -> {
                            Column {
                                AsyncImage(
                                    model = content.url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .sizeIn(maxWidth = 300.dp, maxHeight = 300.dp)
                                        .clip(RoundedCornerShape(RadiusSmall)),
                                    contentScale = ContentScale.Crop,
                                    onError = {
                                        Napier.e(it.result.throwable) { "Coil failed to load chat message image: ${content.url}" }
                                    }
                                )
                                content.caption?.let {
                                    Text(
                                        text = it,
                                        modifier = Modifier.padding(top = 4.dp),
                                        color = if (isMe) Color.White.copy(alpha = 0.7f) else MiraTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMe) {
                val statusIcon = when (message.status) {
                    "READ" -> Icons.Filled.DoneAll
                    "DELIVERED" -> Icons.Filled.DoneAll
                    else -> Icons.Outlined.Check
                }
                val statusColor = if (message.status == "READ") Success else MaterialTheme.colorScheme.tertiary

                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "sent as ${specialistName ?: "Admin"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MiraTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                message.timeFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = ChatTimeText
            )
        }
    }
}

/** Circuit [Ui.Factory] binding [DesktopScreen.Chat] to [AdminChatScreenUi]. */
class AdminChatUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Chat -> ui<AdminChatUiState> { state, _ ->
            AdminChatScreenUi(
                state = state
            )
        }
        else -> null
    }
}
