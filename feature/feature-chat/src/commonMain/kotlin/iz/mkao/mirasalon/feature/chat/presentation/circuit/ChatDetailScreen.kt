package iz.mkao.mirasalon.feature.chat.presentation.circuit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeIntermediate
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeLarge
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeSmall
import iz.mkao.mirasalon.core.designsystem.theme.RadiusMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingDefault
import iz.mkao.mirasalon.core.designsystem.theme.SpacingLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall
import iz.mkao.mirasalon.core.designsystem.theme.SpacingTiny
import iz.mkao.mirasalon.core.designsystem.theme.Success
import iz.mkao.mirasalon.core.designsystem.utils.rememberPermissionHandler
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.feature.chat.domain.model.ChatMessage
import iz.mkao.mirasalon.feature.chat.domain.model.MessageStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailContent(
    state: ChatDetailState,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val permissionHandler = rememberPermissionHandler(
        onPermissionGranted = {
            // Permission granted for whatever action was requested
        },
        onPermissionDenied = {
            // Show toast or dialog
        }
    )

    val groupedMessages = remember(state.messages) {
        state.messages.groupBy { message ->
            val instant = Instant.fromEpochSeconds(message.timestampEpochSeconds)
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            dateTime.date
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            MiraTopAppBar(
                title = state.participantName ?: state.conversationId,
                onBackClick = { state.eventSink(ChatDetailEvent.Back) },
                modifier = Modifier.clickable { state.eventSink(ChatDetailEvent.HeaderClicked) }
            )
        },
        bottomBar = {
            ChatInput(
                text = text,
                onTextChanged = { text = it },
                onSendClicked = {
                    if (text.isNotBlank()) {
                        state.eventSink(ChatDetailEvent.SendMessage(text))
                        text = ""
                    }
                },
                permissionHandler = permissionHandler
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = SpacingMedium, vertical = SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            groupedMessages.forEach { (date, messages) ->
                @OptIn(ExperimentalFoundationApi::class)
                stickyHeader(key = "date_$date") {
                    val dateStr = DateUtils.formatDateSeparator(messages.first().timestampEpochSeconds)
                    DateSeparator(dateStr)
                }
                items(messages, key = { it.id }) { message ->
                    MessageItem(
                        message = message,
                        participantName = state.participantName ?: "Specialist",
                        participantAvatarUrl = state.participantAvatarUrl,
                        currentUserId = state.currentUserId,
                        currentUserName = state.currentUserName ?: "Me",
                        currentUserAvatarUrl = state.currentUserAvatarUrl
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSeparator(dateStr: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = SpacingSmall),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dateStr,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MessageItem(
    message: ChatMessage,
    participantName: String,
    participantAvatarUrl: String?,
    currentUserId: String?,
    currentUserName: String,
    currentUserAvatarUrl: String?
) {
    // Own messages carry the REAL user id (server UUID); anything else is the partner's.
    val isMe = currentUserId != null && message.senderId == currentUserId
    val name = if (isMe) currentUserName else participantName
    val avatarUrl = if (isMe) currentUserAvatarUrl else participantAvatarUrl
    val alignment = if (isMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = SpacingTiny)
        ) {
            if (!isMe) {
                Avatar(url = avatarUrl, name = name)
                Spacer(modifier = Modifier.width(SpacingSmall))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(SpacingSmall))
                Avatar(url = avatarUrl, name = name)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .align(alignment),
            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(horizontalAlignment = alignment) {
                Surface(
                    color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = if (isMe) RadiusMedium else 0.dp,
                        topEnd = if (isMe) 0.dp else RadiusMedium,
                        bottomStart = RadiusMedium,
                        bottomEnd = RadiusMedium
                    ),
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(SpacingMedium),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    modifier = Modifier.padding(top = SpacingTiny),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateUtils.formatTime(message.timestampEpochSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(SpacingTiny))
                        MessageStatusIcon(message.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: MessageStatus) {
    when (status) {
        MessageStatus.SENDING -> {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = "Sending",
                modifier = Modifier.size(IconSizeSmall),
                tint = MaterialTheme.colorScheme.outline
            )
        }
        MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = "Sent",
                modifier = Modifier.size(IconSizeSmall),
                tint = MaterialTheme.colorScheme.outline
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Filled.DoneAll,
                contentDescription = "Delivered",
                modifier = Modifier.size(IconSizeSmall),
                tint = MaterialTheme.colorScheme.outline
            )
        }
        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Filled.DoneAll,
                contentDescription = "Read",
                modifier = Modifier.size(IconSizeSmall),
                tint = Success
            )
        }
    }
}

@Composable
private fun Avatar(url: String?, name: String) {
    val resolvedUrl = ApiEndpoints.resolveImageUrl(url)
    AsyncImage(
        model = resolvedUrl,
        contentDescription = name,
        modifier = Modifier
            .size(IconSizeIntermediate)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun ChatInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    permissionHandler: iz.mkao.mirasalon.core.designsystem.utils.PermissionHandler
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(SpacingLarge))
                .padding(horizontal = SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = { Text("Type here..", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) {
                            onSendClicked()
                        }
                    }
                ),
                singleLine = true
            )
            Icon(
                imageVector = Icons.Outlined.AttachFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).clickable {
                    if (permissionHandler.hasGalleryPermission()) {
                        // Launch file picker
                    } else {
                        permissionHandler.requestGalleryPermission()
                    }
                }
            )
            Spacer(modifier = Modifier.width(SpacingDefault))
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).clickable {
                    if (permissionHandler.hasCameraPermission()) {
                        // Launch camera
                    } else {
                        permissionHandler.requestCameraPermission()
                    }
                }
            )
        }
        Spacer(modifier = Modifier.width(SpacingDefault))
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSendClicked()
                } else {
                    if (permissionHandler.hasRecordAudioPermission()) {
                        // Start voice recording
                    } else {
                        permissionHandler.requestRecordAudio()
                    }
                }
            },
            modifier = Modifier
                .size(IconSizeLarge)
                .clip(CircleShape)
                .background(if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            enabled = true
        ) {
            Icon(
                imageVector = if (text.isNotBlank()) Icons.AutoMirrored.Outlined.Send else Icons.Outlined.Mic,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

