package iz.mkao.mirasalon.feature.chat.presentation.circuit

import androidx.compose.runtime.*
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.feature.chat.domain.model.ChatMessage
import iz.mkao.mirasalon.feature.chat.domain.repository.ChatRepository
import iz.mkao.mirasalon.core.navigation.ChatRoute
import iz.mkao.mirasalon.core.navigation.SpecialistRoute
import kotlinx.coroutines.launch

class ChatDetailPresenter(
    private val screen: ChatRoute.ChatDetail,
    private val repository: ChatRepository,
    private val navigator: Navigator
) : Presenter<ChatDetailState> {

    @Composable
    override fun present(): ChatDetailState {
        val messages by repository.observeMessages(screen.conversationId).collectAsState(initial = emptyList())
        val conversations by repository.observeConversations().collectAsState(initial = emptyList())
        val conversation = remember(conversations) {
            conversations.find { it.id == screen.conversationId }
        }
        var currentUserId by remember { mutableStateOf<String?>(null) }
        var currentUserName by remember { mutableStateOf<String?>(null) }
        var currentUserAvatarUrl by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            currentUserId = repository.getCurrentUserId()
            currentUserName = repository.getCurrentUserName()
            currentUserAvatarUrl = repository.getCurrentUserAvatarUrl()
            repository.markAsRead(screen.conversationId)
        }

        val specialistId = remember(screen.conversationId) {
            ChatUtils.parseParticipantIds(screen.conversationId)
                ?.firstOrNull { it.startsWith("spec-") }
                ?: screen.conversationId
        }

        return ChatDetailState(
            conversationId = screen.conversationId,
            participantId = specialistId,
            participantName = conversation?.participantName ?: screen.participantName ?: screen.conversationId,
            participantAvatarUrl = conversation?.participantImageUrl ?: screen.participantAvatarUrl,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            currentUserAvatarUrl = currentUserAvatarUrl,
            messages = messages,
            eventSink = { event ->
                when (event) {
                    ChatDetailEvent.Back -> navigator.pop()
                    is ChatDetailEvent.SendMessage -> scope.launch {
                        repository.sendMessage(screen.conversationId, event.text)
                    }
                    ChatDetailEvent.HeaderClicked -> {
                        navigator.goTo(SpecialistRoute.SpecialistDetail(specialistId = specialistId))
                    }
                }
            }
        )
    }
}

