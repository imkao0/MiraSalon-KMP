package iz.mkao.mirasalon.feature.specialists.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.navigation.BookingRoute
import iz.mkao.mirasalon.core.navigation.ChatRoute
import iz.mkao.mirasalon.core.navigation.SpecialistRoute
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import kotlinx.coroutines.launch

class SpecialistDetailPresenter(
    private val screen: SpecialistRoute.SpecialistDetail,
    private val repository: SpecialistRepository,
    private val navigator: Navigator,
    private val tokenProvider: SalonTokenProvider
) : Presenter<SpecialistDetailState> {

    @Composable
    override fun present(): SpecialistDetailState {
        val scope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(true) }
        var specialist by remember { mutableStateOf<iz.mkao.mirasalon.core.domain.model.Specialist?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var showReviewSheet by remember { mutableStateOf(false) }

        fun loadSpecialist() {
            scope.launch {
                isLoading = true
                when (val outcome = repository.getSpecialist(screen.specialistId)) {
                    is Outcome.Success -> {
                        specialist = outcome.data
                        error = null
                    }
                    is Outcome.Error -> {
                        error = "Failed to load specialist details"
                    }
                    else -> {}
                }
                isLoading = false
            }
        }

        LaunchedEffect(screen.specialistId) {
            loadSpecialist()
        }

        return SpecialistDetailState(
            isLoading = isLoading,
            specialist = specialist,
            error = error,
            showReviewSheet = showReviewSheet,
            onReviewSubmit = { rating, comment ->
                val result = repository.submitReview(screen.specialistId, rating, comment, null)
                if (result is Outcome.Success) {
                    loadSpecialist()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(result.toString()))
                }
            },
            eventSink = { event ->
                when (event) {
                    SpecialistDetailEvent.Back -> navigator.pop()
                    SpecialistDetailEvent.BookAppointmentClicked -> {
                        specialist?.services?.firstOrNull()?.let { firstService ->
                            navigator.goTo(
                                BookingRoute.Booking(
                                    serviceIds = listOf(firstService.id),
                                    specialistId = screen.specialistId
                                )
                            )
                        }
                    }
                    is SpecialistDetailEvent.BookServiceClicked -> {
                        navigator.goTo(
                            BookingRoute.Booking(
                                serviceIds = listOf(event.serviceId),
                                specialistId = screen.specialistId
                            )
                        )
                    }
                    is SpecialistDetailEvent.ChatClicked -> {
                        scope.launch {
                            val currentUserId = tokenProvider.userId() ?: "me"
                            val deterministicId = ChatUtils.getDeterministicChatId(currentUserId, event.specialist.id)
                            navigator.goTo(
                                ChatRoute.ChatDetail(
                                    conversationId = deterministicId,
                                    participantName = event.specialist.name,
                                    participantAvatarUrl = event.specialist.imageUrl,
                                    participantId = event.specialist.id
                                )
                            )
                        }
                    }
                    SpecialistDetailEvent.SaveClicked -> {
                    }
                    SpecialistDetailEvent.DismissReviewSheet -> showReviewSheet = false
                    SpecialistDetailEvent.WriteReviewClicked -> showReviewSheet = true
                }
            }
        )
    }
}
