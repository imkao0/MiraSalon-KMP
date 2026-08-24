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
import kotlin.time.Clock

class PaymentSuccessPresenter(
    private val screen: BookingRoute.PaymentSuccess,
    private val repository: BookingRepository,
    private val navigator: Navigator
) : Presenter<PaymentSuccessState> {

    @Composable
    override fun present(): PaymentSuccessState {
        var booking by remember { mutableStateOf<ConfirmedBooking?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        val currentTimeMillis = remember<Long> { Clock.System.now().toEpochMilliseconds() }

        LaunchedEffect(screen.appointmentId) {
            isLoading = true
            booking = repository.getBookingById(screen.appointmentId)
            isLoading = false
        }

        return PaymentSuccessState(
            booking = booking,
            isLoading = isLoading,
            currentTimeMillis = currentTimeMillis,
            eventSink = { event ->
                when (event) {
                    PaymentSuccessEvent.BackToHome -> navigator.resetRoot(BottomNavKey.Home())
                    PaymentSuccessEvent.Continue -> navigator.resetRoot(BottomNavKey.Booking())
                    PaymentSuccessEvent.ViewReceipt -> {
                        println("PaymentSuccessPresenter: Navigating to EReceipt for ID: ${screen.appointmentId}")
                        navigator.goTo(BookingRoute.EReceipt(screen.appointmentId))
                    }
                }
            }
        )
    }
}
