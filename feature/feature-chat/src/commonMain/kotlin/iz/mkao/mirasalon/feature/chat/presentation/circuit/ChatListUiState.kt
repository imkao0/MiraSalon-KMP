package iz.mkao.mirasalon.feature.chat.presentation.circuit

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.chat.domain.model.ChatItem
import iz.mkao.mirasalon.feature.chat.domain.model.QuickAccessContact

@Immutable
sealed interface ChatListState : CircuitUiState {
    data object Loading : ChatListState

    data class Error(val message: String) : ChatListState

    data class Content(
        val currentUserAvatarUrl: String?,
        val quickAccessContacts: List<QuickAccessContact>,
        val chats: List<ChatItem>,
        val searchQuery: String,
        val eventSink: (ChatListEvent) -> Unit,
    ) : ChatListState
}

sealed interface ChatListEvent : CircuitUiEvent {
    data class OpenChat(val chatId: String) : ChatListEvent
    data class OpenSearch(val query: String = "") : ChatListEvent
    data class OpenQuickContact(val contactId: String) : ChatListEvent
    data object OpenProfile : ChatListEvent
    data object OpenOptions : ChatListEvent
    data object DeleteHistory : ChatListEvent
    data object Back : ChatListEvent
}
