package iz.mkao.mirasalon.feature.appointments.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.navigation.AppointmentRoute
import iz.mkao.mirasalon.core.navigation.SpecialistRoute
import iz.mkao.mirasalon.feature.appointments.domain.model.Appointment
import iz.mkao.mirasalon.feature.appointments.domain.repository.AppointmentRepository
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AppointmentsPresenter(
    private val repository: AppointmentRepository,
    private val navigator: Navigator,
) : Presenter<AppointmentsState> {

    @Composable
    override fun present(): AppointmentsState {
        var isLoading by remember { mutableStateOf(true) }
        var appointments by remember { mutableStateOf(emptyList<Appointment>()) }
        var error by remember { mutableStateOf<String?>(null) }
        val currentTimeMillis = remember<Long> { Clock.System.now().toEpochMilliseconds() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            repository.observeAppointments().collect { outcome ->
                when (outcome) {
                    is Outcome.Success -> {
                        isLoading = false
                        appointments = outcome.data
                        error = null
                    }
                    is Outcome.Error -> {
                        isLoading = false
                        error = "Failed to load appointments"
                    }
                    is Outcome.Loading -> isLoading = true
                }
            }
        }

        val grouped = remember(appointments) {
            appointments.sortedByDescending { it.dateTime }.groupBy { appointment ->
                val instant = Instant.fromEpochMilliseconds(appointment.dateTime)
                val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                val month = localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }
                "$month ${localDateTime.year}"
            }
        }

        return AppointmentsState(
            isLoading = isLoading,
            groupedAppointments = grouped,
            currentTimeMillis = currentTimeMillis,
            error = error,
            eventSink = { event ->
                when (event) {
                    AppointmentsEvent.Refresh -> scope.launch { repository.refreshAppointments() }
                    AppointmentsEvent.Back -> navigator.pop()
                    is AppointmentsEvent.AppointmentClicked -> {
                        navigator.goTo(AppointmentRoute.AppointmentDetail(event.id))
                    }
                    is AppointmentsEvent.SpecialistClicked -> {
                        navigator.goTo(SpecialistRoute.SpecialistDetail(event.id))
                    }
                    is AppointmentsEvent.CancelAppointment -> {
                        scope.launch { repository.cancelAppointment(event.id) }
                    }
                }
            }
        )
    }
}
