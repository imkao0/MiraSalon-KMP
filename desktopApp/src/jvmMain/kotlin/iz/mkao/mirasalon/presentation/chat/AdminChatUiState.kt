package iz.mkao.mirasalon.presentation.chat

import androidx.compose.ui.graphics.ImageBitmap
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.model.chat.ChatMessage
import iz.mkao.mirasalon.core.domain.model.chat.ChatSession

data class AdminChatUiState(
    val currentUserId: String? = null,
    val userName: String? = null,
    val userAvatar: String? = null,
    val specialists: List<Specialist> = emptyList(),
    val selectedSpecialistId: String? = null,
    val selectedSessionId: String? = null,
    val filteredChannels: List<ChatSession> = emptyList(),
    val selectedSession: ChatSession? = null,
    val specialistUnreadCounts: Map<String, Int> = emptyMap(),
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val pendingImageBytes: ByteArray? = null,
    val pendingImageName: String? = null,
    val pendingImagePreview: ImageBitmap? = null,
    val isSendingImage: Boolean = false,
    val clearedChannelIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventSink: (AdminChatEvent) -> Unit = {}
) : CircuitUiState {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdminChatUiState

        if (isSendingImage != other.isSendingImage) return false
        if (isLoading != other.isLoading) return false
        if (userName != other.userName) return false
        if (userAvatar != other.userAvatar) return false
        if (specialists != other.specialists) return false
        if (selectedSpecialistId != other.selectedSpecialistId) return false
        if (selectedSessionId != other.selectedSessionId) return false
        if (filteredChannels != other.filteredChannels) return false
        if (selectedSession != other.selectedSession) return false
        if (messages != other.messages) return false
        if (inputText != other.inputText) return false
        if (!pendingImageBytes.contentEquals(other.pendingImageBytes)) return false
        if (pendingImageName != other.pendingImageName) return false
        if (pendingImagePreview != other.pendingImagePreview) return false
        if (clearedChannelIds != other.clearedChannelIds) return false
        if (error != other.error) return false
        if (eventSink != other.eventSink) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isSendingImage.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + (userName?.hashCode() ?: 0)
        result = 31 * result + (userAvatar?.hashCode() ?: 0)
        result = 31 * result + specialists.hashCode()
        result = 31 * result + (selectedSpecialistId?.hashCode() ?: 0)
        result = 31 * result + (selectedSessionId?.hashCode() ?: 0)
        result = 31 * result + filteredChannels.hashCode()
        result = 31 * result + (selectedSession?.hashCode() ?: 0)
        result = 31 * result + messages.hashCode()
        result = 31 * result + inputText.hashCode()
        result = 31 * result + (pendingImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (pendingImageName?.hashCode() ?: 0)
        result = 31 * result + (pendingImagePreview?.hashCode() ?: 0)
        result = 31 * result + clearedChannelIds.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + eventSink.hashCode()
        return result
    }
}

/** Circuit UI events for the admin chat screen. */
sealed interface AdminChatEvent : CircuitUiEvent {
    data class SelectSpecialist(val id: String) : AdminChatEvent
    data class SelectSession(val id: String) : AdminChatEvent
    data class NotifyChatReply(val targetUserId: String, val senderName: String, val conversationId: String? = null) : AdminChatEvent
    data class InputTextChanged(val text: String) : AdminChatEvent
    /** Admin picked an image to attach; [preview] is used for the in-composer thumbnail. */
    data class ImageSelected(val bytes: ByteArray, val fileName: String, val preview: ImageBitmap?) : AdminChatEvent
    data object ClearPendingImage : AdminChatEvent
    data object SendMessage : AdminChatEvent
}
