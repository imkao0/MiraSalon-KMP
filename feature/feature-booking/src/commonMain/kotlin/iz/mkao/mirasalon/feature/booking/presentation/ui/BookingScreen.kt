package iz.mkao.mirasalon.feature.booking.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.navigation.BookingRoute
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.feature.booking.presentation.circuit.AppointmentCheckoutState
import iz.mkao.mirasalon.feature.booking.presentation.circuit.BookingEvent
import iz.mkao.mirasalon.feature.booking.presentation.circuit.BookingState
import iz.mkao.mirasalon.feature.booking.presentation.circuit.EReceiptState
import iz.mkao.mirasalon.feature.booking.presentation.circuit.MyBookingsState
import iz.mkao.mirasalon.feature.booking.presentation.circuit.PaymentSuccessState
import iz.mkao.mirasalon.feature.booking.presentation.ui.components.BookingDropdownSheet
import iz.mkao.mirasalon.feature.booking.presentation.ui.components.CalendarSection
import iz.mkao.mirasalon.feature.booking.presentation.ui.components.SpecialistSection
import iz.mkao.mirasalon.feature.booking.presentation.ui.components.SummaryAndBookBar
import iz.mkao.mirasalon.feature.booking.presentation.ui.components.TimeSlotSection
import iz.mkao.mirasalon.feature.booking.presentation.ui.receipt.EReceiptUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingUi(
    state: BookingState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MiraTopAppBar(title = "Book Appointment", onBackClick = { state.eventSink(BookingEvent.Back) })
        },
        bottomBar = {
            SummaryAndBookBar(
                state = state,
                onBook = {
                    state.eventSink(BookingEvent.Continue)
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            CalendarSection(
                state = state,
                onDateSelected = { state.eventSink(BookingEvent.DateSelected(it)) },
                onToggle = { state.eventSink(BookingEvent.ToggleCalendar) }
            )

            BookingDropdownSheet(
                expanded = state.sheetExpanded,
                date = state.selectedDate,
                bookings = state.selectedDateBookings
            )

            Spacer(modifier = Modifier.height(24.dp))

            SpecialistSection(
                state = state,
                onSpecialistSelected = {
                    state.eventSink(BookingEvent.SpecialistSelected(it))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            TimeSlotSection(
                state = state,
                onSlotSelected = {
                    state.eventSink(BookingEvent.SlotSelected(it))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

class BookingManualUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is BookingRoute.Booking -> ui<BookingState> { state, modifier -> BookingUi(state, modifier) }
            is BookingRoute.AppointmentCheckout -> ui<AppointmentCheckoutState> { state, modifier -> AppointmentCheckoutUi(state, modifier) }
            is BookingRoute.PaymentSuccess -> ui<PaymentSuccessState> { state, modifier -> PaymentSuccessUi(state, modifier) }
            is BottomNavKey.Booking -> ui<MyBookingsState> { state, modifier -> MyBookingsUi(state, modifier) }
            is BookingRoute.EReceipt -> ui<EReceiptState> { state, modifier -> EReceiptUi(state, modifier) }
            else -> null
        }
    }
}
