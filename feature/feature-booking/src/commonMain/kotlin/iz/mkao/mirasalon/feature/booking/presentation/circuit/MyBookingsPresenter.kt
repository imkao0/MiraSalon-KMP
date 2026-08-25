package iz.mkao.mirasalon.feature.booking.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.navigation.BookingRoute
import iz.mkao.mirasalon.feature.booking.data.repository.BookingRepository
import iz.mkao.mirasalon.feature.booking.domain.model.BookingStatus
import kotlinx.coroutines.launch

class MyBookingsPresenter(
    private val repository: BookingRepository,
    private val navigator: Navigator
) : Presenter<MyBookingsState> {

    @Composable
    override fun present(): MyBookingsState {
        val bookings by repository.confirmedBookings.collectAsState(initial = emptyList())
        var selectedStatus by remember { mutableStateOf(BookingStatus.Confirmed) }
        var isLoading by remember { mutableStateOf(false) }
        var showReviewSheet by remember { mutableStateOf(false) }
        var reviewBookingId by remember { mutableStateOf<String?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        // Auto-refresh bookings when screen is first loaded
        LaunchedEffect(Unit) {
            if (bookings.isEmpty()) {
                isLoading = true
            }
            repository.refreshBookings()
            isLoading = false
        }

        return MyBookingsState(
            bookings = bookings,
            selectedStatus = selectedStatus,
            isLoading = isLoading,
            showReviewSheet = showReviewSheet,
            reviewBookingId = reviewBookingId,
            currentTimeMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            errorMessage = errorMessage,
            onReviewSubmit = { rating, comment ->
                val bookingId = reviewBookingId
                if (bookingId != null) {
                    repository.submitReview(bookingId, rating, comment)
                } else {
                    Result.failure(Exception("No booking selected"))
                }
            },
            eventSink = { event ->
                when (event) {
                    MyBookingsEvent.Back -> navigator.pop()
                    is MyBookingsEvent.TabSelected -> selectedStatus = event.status
                    is MyBookingsEvent.ReminderToggled -> {
                        scope.launch {
                            repository.updateReminderEnabled(event.id, event.enabled)
                        }
                    }
                    is MyBookingsEvent.EReceiptClicked -> 
                        navigator.goTo(BookingRoute.EReceipt(event.id))
                    is MyBookingsEvent.CancelClicked -> {
                        val booking = bookings.find { it.id == event.id }
                        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                        
                        if (booking != null && !booking.canCancel(now)) {
                            errorMessage = "Bookings can only be cancelled at least 48 hours in advance."
                        } else {
                            scope.launch {
                                val result = repository.cancelBooking(event.id)
                                if (result.isFailure) {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to cancel booking"
                                }
                            }
                        }
                    }
                    is MyBookingsEvent.RebookClicked -> 
                        navigator.goTo(BookingRoute.Booking(serviceIds = event.booking.services.map { it.id }))
                    is MyBookingsEvent.AddReviewClicked -> {
                        reviewBookingId = event.id
                        showReviewSheet = true
                    }
                    MyBookingsEvent.DismissReviewSheet -> {
                        showReviewSheet = false
                        reviewBookingId = null
                    }
                    MyBookingsEvent.DismissError -> {
                        errorMessage = null
                    }
                    MyBookingsEvent.Refresh -> {
                        scope.launch {
                            isLoading = true
                            repository.refreshBookings()
                            isLoading = false
                        }
                    }
                }
            }
        )
    }
}
