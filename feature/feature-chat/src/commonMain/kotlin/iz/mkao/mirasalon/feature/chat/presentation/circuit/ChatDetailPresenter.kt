package iz.mkao.mirasalon.feature.chat.presentation.circuit

import androidx.compose.runtime.*
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.feature.chat.domain.model.ChatMessage
import iz.mkao.mirasalon.feature.chat.domain.repository.ChatRepository
import iz.mkao.mirasalon.core.navigation.ChatRoute
import iz.mkao.mirasalon.core.navigation.SpecialistRoute
import kotlinx.coroutines.launch

class ChatDetailPresenter(
    private val screen: ChatRoute.ChatDetail,
    private val repository: ChatRepository,
    private val specialistRepository: SpecialistRepository,
    private val navigator: Navigator
) : Presenter<ChatDetailState> {

    @Composable
    override fun present(): ChatDetailState {
        val messagesFlow = remember(screen.conversationId) { repository.observeMessages(screen.conversationId) }
        val messages by messagesFlow.collectAsState(initial = emptyList())
        val conversations by repository.observeConversations().collectAsState(initial = emptyList())
        val conversation = remember(conversations) {
            conversations.find { it.id == screen.conversationId }
        }
        var currentUserId by remember { mutableStateOf<String?>(null) }
        var currentUserName by remember { mutableStateOf<String?>(null) }
        var currentUserAvatarUrl by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        val specialistId = remember(screen.conversationId, conversation) {
            // First try to get specialist ID from route (provided when navigating from specialist detail)
            screen.participantId
                // Then try to get specialist ID from conversation's participantIds
                ?: conversation?.participantIds?.firstOrNull { it != currentUserId }
                // Then try to parse from chat ID
                ?: ChatUtils.parseParticipantIds(screen.conversationId)
                    ?.firstOrNull { it != currentUserId }
                // If chat ID is hashed, we can't parse it - don't make API call
                ?: null
        }

        val specialistOutcome = if (specialistId != null) {
            specialistRepository.observeSpecialist(specialistId).collectAsState(initial = Outcome.Loading).value
        } else {
            Outcome.Loading
        }
        val specialist = (specialistOutcome as? Outcome.Success)?.data

        LaunchedEffect(Unit) {
            currentUserId = repository.getCurrentUserId()
            currentUserName = repository.getCurrentUserName()
            currentUserAvatarUrl = repository.getCurrentUserAvatarUrl()
            repository.markAsRead(screen.conversationId)
        }

        return ChatDetailState(
            conversationId = screen.conversationId,
            participantId = specialistId,
            participantName = specialist?.name ?: conversation?.participantName ?: screen.participantName ?: screen.conversationId,
            participantAvatarUrl = specialist?.imageUrl ?: conversation?.participantImageUrl ?: screen.participantAvatarUrl,
            participantRole = specialist?.role ?: conversation?.participantRole,
            isOnline = specialist?.isOnline ?: false,
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
                        specialistId?.let { id ->
                            navigator.goTo(SpecialistRoute.SpecialistDetail(specialistId = id))
                        }
                    }
                }
            }
        )
    }
}

