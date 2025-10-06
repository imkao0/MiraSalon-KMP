package iz.mkao.mirasalon.presentation.reviews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.ReviewsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReviewsPresenter(
    private val repository: ReviewsRepository
) : Presenter<ReviewsUiState> {

    @Composable
    override fun present(): ReviewsUiState {
        var reviews by remember { mutableStateOf(ReviewsUiState().reviews) }
        var searchQuery by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        var loadJob by remember { mutableStateOf<Job?>(null) }
        var actionJob by remember { mutableStateOf<Job?>(null) }

        fun loadReviews(query: String) {
            loadJob?.cancel()
            loadJob = scope.launch {
                isLoading = true
                val result = repository.getAll(query = query.ifBlank { null })
                if (result is Outcome.Success) reviews = result.data
                isLoading = false
            }
        }

        LaunchedEffect(Unit) { loadReviews(searchQuery) }

        return ReviewsUiState(
            reviews = reviews,
            searchQuery = searchQuery,
            isLoading = isLoading
        ) { event ->
            when (event) {
                is ReviewsEvent.Search -> {
                    searchQuery = event.query
                    loadReviews(event.query)
                }
                ReviewsEvent.Refresh -> loadReviews(searchQuery)
                is ReviewsEvent.ToggleVisibility -> {
                    actionJob?.cancel()
                    actionJob = scope.launch {
                        isLoading = true
                        try {
                            when (repository.updateVisibility(event.id, event.isVisible)) {
                                is Outcome.Success -> loadReviews(searchQuery)
                                else -> isLoading = false
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            isLoading = false
                        }
                    }
                }
                is ReviewsEvent.DeleteReview -> {
                    actionJob?.cancel()
                    actionJob = scope.launch {
                        isLoading = true
                        try {
                            when (repository.delete(event.id)) {
                                is Outcome.Success -> loadReviews(searchQuery)
                                else -> isLoading = false
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            isLoading = false
                        }
                    }
                }
                is ReviewsEvent.Reply -> {
                    actionJob?.cancel()
                    actionJob = scope.launch {
                        isLoading = true
                        try {
                            when (repository.postReply(event.id, event.reply)) {
                                is Outcome.Success -> loadReviews(searchQuery)
                                else -> isLoading = false
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            isLoading = false
                        }
                    }
                }
            }
        }
    }
}
