package iz.mkao.mirasalon.feature.appointments.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.MiraEmptyState
import iz.mkao.mirasalon.feature.appointments.presentation.circuit.AppointmentsEvent
import iz.mkao.mirasalon.feature.appointments.presentation.circuit.AppointmentsState
import iz.mkao.mirasalon.feature.appointments.presentation.screen.components.AppointmentItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppointmentsContent(
    state: AppointmentsState
) {
    Scaffold(
        topBar = {
            MiraTopAppBar(
                title = "Appointments",
                onBackClick = { state.eventSink(AppointmentsEvent.Back) }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { state.eventSink(AppointmentsEvent.Refresh) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            if (state.groupedAppointments.isEmpty() && !state.isLoading) {
                MiraEmptyState(
                    message = "No appointments found",
                    description = "You don't have any appointments scheduled yet. Book your first appointment to see it here.",
                    icon = Icons.Outlined.CalendarToday
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(SpacingMedium)
                ) {
                    state.groupedAppointments.forEach { (header, items) ->
                        stickyHeader {
                            HeaderSection(title = header)
                        }
                        items(items) { appointment ->
                            AppointmentItem(
                                appointment = appointment,
                                currentTimeMillis = state.currentTimeMillis,
                                onClick = { state.eventSink(AppointmentsEvent.AppointmentClicked(appointment.id)) },
                                onSpecialistClick = { state.eventSink(AppointmentsEvent.SpecialistClicked(it)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = SpacingSmall)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
