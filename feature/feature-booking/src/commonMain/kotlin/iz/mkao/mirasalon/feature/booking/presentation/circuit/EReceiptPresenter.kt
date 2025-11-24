package iz.mkao.mirasalon.feature.booking.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.navigation.BookingRoute
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.feature.booking.data.repository.BookingRepository
import iz.mkao.mirasalon.feature.booking.domain.model.ConfirmedBooking

class EReceiptPresenter(
    private val screen: BookingRoute.EReceipt,
    private val repository: BookingRepository,
    private val navigator: Navigator,
) : Presenter<EReceiptState> {

    @Composable
    override fun present(): EReceiptState {
        var booking by remember { mutableStateOf<ConfirmedBooking?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(screen.appointmentId) {
            isLoading = true
            booking = repository.getBookingById(screen.appointmentId)
            isLoading = false
        }

        val eventSink: (EReceiptEvent) -> Unit = { event ->
            when (event) {
                EReceiptEvent.CloseClicked -> navigator.resetRoot(BottomNavKey.Booking())
                EReceiptEvent.BackClicked -> navigator.pop()
                EReceiptEvent.ViewBookingsClicked -> navigator.resetRoot(BottomNavKey.Booking())
            }
        }

        val currentBooking = booking
        return when {
            isLoading -> EReceiptState.Loading
            currentBooking == null -> EReceiptState.Error("Receipt not found", eventSink)
            else -> EReceiptState.Success(
                bookingId = screen.appointmentId,
                booking = currentBooking,
                customerEmail = currentBooking.customerEmail,
                eventSink = eventSink
            )
        }
    }
}
