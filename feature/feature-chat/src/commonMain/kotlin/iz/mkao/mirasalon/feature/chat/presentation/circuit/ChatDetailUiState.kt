package iz.mkao.mirasalon.feature.chat.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.chat.domain.model.ChatMessage

data class ChatDetailState(
    val conversationId: String,
    val participantId: String? = null,
    val participantName: String? = null,
    val participantAvatarUrl: String? = null,
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val currentUserAvatarUrl: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val eventSink: (ChatDetailEvent) -> Unit = {}
) : CircuitUiState

sealed interface ChatDetailEvent : CircuitUiEvent {
    data object Back : ChatDetailEvent
    data class SendMessage(val text: String) : ChatDetailEvent
    data object HeaderClicked : ChatDetailEvent
}
