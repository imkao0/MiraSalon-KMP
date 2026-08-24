package iz.mkao.mirasalon.feature.chat.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.ChatRoute
import iz.mkao.mirasalon.feature.chat.domain.model.ChatItem
import iz.mkao.mirasalon.feature.chat.domain.model.DeliveryStatus
import iz.mkao.mirasalon.feature.chat.domain.model.QuickAccessContact
import iz.mkao.mirasalon.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.launch

class ChatListPresenter(
    private val chatRepository: ChatRepository,
    private val specialistRepository: SpecialistRepository,
    private val tokenProvider: SalonTokenProvider,
    private val navigator: Navigator
) : Presenter<ChatListState> {

    @Composable
    override fun present(): ChatListState {
        val conversations by chatRepository.observeConversations().collectAsState(initial = emptyList())
        val specialistsOutcome by specialistRepository.observeSpecialists().collectAsState(initial = Outcome.Loading)
        
        var currentUserAvatarUrl by remember { mutableStateOf<String?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            currentUserAvatarUrl = chatRepository.getCurrentUserAvatarUrl()
        }

        if (specialistsOutcome is Outcome.Loading && conversations.isEmpty()) {
            return ChatListState.Loading
        }

        val quickAccessContacts = (specialistsOutcome as? Outcome.Success)?.data?.map { specialist ->
            QuickAccessContact(
                id = specialist.id,
                name = specialist.name,
                role = specialist.role,
                avatarUrl = specialist.imageUrl
            )
        } ?: emptyList()

        val chats = conversations.map { conversation ->
            // conversation.id is the deterministic chat id (chat(customer, specialist)).
            // Resolve the specialist from the participant ids encoded in that chat id,
            // falling back to a direct id match for legacy conversations.
            val participantIds = ChatUtils.parseParticipantIds(conversation.id) ?: emptyList()
            val specialist = (specialistsOutcome as? Outcome.Success)?.data?.find { spec ->
                spec.id == conversation.id || spec.id in participantIds
            }
            ChatItem(
                id = conversation.id,
                contactName = specialist?.name ?: conversation.participantName,
                contactRole = specialist?.role ?: conversation.participantRole,
                avatarUrl = specialist?.imageUrl ?: conversation.participantImageUrl,
                lastMessage = conversation.lastMessage?.text ?: "No messages",
                timestamp = conversation.lastMessage?.let { 
                    DateUtils.formatTime24Hour(it.timestampEpochSeconds)
                } ?: "",
                unreadCount = conversation.unreadCount,
                isOnline = specialist?.isOnline ?: false,
                deliveryStatus = DeliveryStatus.Sent
            )
        }

        return ChatListState.Content(
            currentUserAvatarUrl = currentUserAvatarUrl,
            quickAccessContacts = quickAccessContacts,
            chats = chats,
            searchQuery = searchQuery,
            eventSink = { event ->
                when (event) {
                    is ChatListEvent.OpenChat -> {
                        scope.launch { chatRepository.markAsRead(event.chatId) }
                        val chatItem = chats.find { it.id == event.chatId }
                        navigator.goTo(
                            ChatRoute.ChatDetail(
                                conversationId = event.chatId,
                                participantName = chatItem?.contactName,
                                participantAvatarUrl = chatItem?.avatarUrl
                            )
                        )
                    }
                    is ChatListEvent.OpenSearch -> searchQuery = event.query
                    is ChatListEvent.OpenQuickContact -> {
                        scope.launch { 
                            chatRepository.markAsRead(event.contactId)
                            val currentUserId = tokenProvider.userId() ?: "me"
                            val deterministicId = ChatUtils.getDeterministicChatId(currentUserId, event.contactId)
                            
                            val contact = (specialistsOutcome as? Outcome.Success)?.data?.find { it.id == event.contactId }
                            navigator.goTo(
                                ChatRoute.ChatDetail(
                                    conversationId = deterministicId,
                                    participantName = contact?.name,
                                    participantAvatarUrl = contact?.imageUrl
                                )
                            )
                        }
                    }
                    ChatListEvent.OpenProfile -> navigator.goTo(BottomNavKey.Profile())
                    ChatListEvent.OpenOptions -> {}
                    ChatListEvent.DeleteHistory -> {
                        scope.launch { chatRepository.deleteHistory() }
                    }
                    is ChatListEvent.DeleteChat -> {
                        scope.launch { chatRepository.deleteChat(event.chatId) }
                    }
                    ChatListEvent.Back -> navigator.pop()
                }
            },
        )
    }

}

class ChatManualPresenterFactory(
    private val chatRepository: ChatRepository,
    private val specialistRepository: SpecialistRepository,
    private val tokenProvider: SalonTokenProvider
) : Presenter.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is ChatRoute.ChatList -> ChatListPresenter(chatRepository, specialistRepository, tokenProvider, navigator)
            is BottomNavKey.Chat -> ChatListPresenter(chatRepository, specialistRepository, tokenProvider, navigator)
            is ChatRoute.ChatDetail -> ChatDetailPresenter(screen, chatRepository, navigator)
            else -> null
        }
    }
}
