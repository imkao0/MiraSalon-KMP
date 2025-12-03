package iz.mkao.mirasalon.feature.appointments.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.navigation.AppointmentRoute
import iz.mkao.mirasalon.core.navigation.SpecialistRoute
import iz.mkao.mirasalon.feature.appointments.domain.model.Appointment
import iz.mkao.mirasalon.feature.appointments.domain.repository.AppointmentRepository
import iz.mkao.mirasalon.feature.appointments.presentation.screen.AppointmentDetailContent
import kotlinx.coroutines.launch

class AppointmentDetailPresenter(
    private val screen: AppointmentRoute.AppointmentDetail,
    private val repository: AppointmentRepository,
    private val navigator: Navigator,
) : Presenter<AppointmentDetailState> {

    @Composable
    override fun present(): AppointmentDetailState {
        var appointment by remember { mutableStateOf<Appointment?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(screen.appointmentId) {
            repository.observeAppointments().collect { outcome ->
                when (outcome) {
                    is Outcome.Success -> {
                        appointment = outcome.data.find { it.id == screen.appointmentId }
                        isLoading = false
                        if (appointment == null) error = "Appointment not found"
                    }
                    is Outcome.Error -> {
                        isLoading = false
                        error = "Failed to load appointment"
                    }
                    is Outcome.Loading -> isLoading = true
                }
            }
        }

        return AppointmentDetailState(
            appointment = appointment,
            isLoading = isLoading,
            error = error,
            eventSink = { event ->
                when (event) {
                    AppointmentDetailEvent.Back -> navigator.pop()
                    AppointmentDetailEvent.Cancel -> {
                        scope.launch { repository.cancelAppointment(screen.appointmentId) }
                    }
                    AppointmentDetailEvent.ViewMap -> {}
                    is AppointmentDetailEvent.SpecialistClicked -> {
                        navigator.goTo(SpecialistRoute.SpecialistDetail(event.specialistId))
                    }
                }
            }
        )
    }
}

class AppointmentDetailManualFactory(
    private val repository: AppointmentRepository
) : Presenter.Factory, Ui.Factory {
    override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
        return when (screen) {
            is AppointmentRoute.Appointments -> AppointmentsPresenter(repository, navigator)
            is AppointmentRoute.AppointmentDetail -> AppointmentDetailPresenter(screen, repository, navigator)
            else -> null
        }
    }

    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is AppointmentRoute.AppointmentDetail -> ui<AppointmentDetailState> { state, modifier ->
                state.appointment?.let { appointment ->
                    AppointmentDetailContent(
                        appointment = appointment,
                        onBackClick = { state.eventSink(AppointmentDetailEvent.Back) },
                        onCancelClick = { state.eventSink(AppointmentDetailEvent.Cancel) },
                        onMapClick = { state.eventSink(AppointmentDetailEvent.ViewMap) },
                        onSpecialistClick = { state.eventSink(AppointmentDetailEvent.SpecialistClicked(it)) }
                    )
                }
            }
            else -> null
        }
    }
}
