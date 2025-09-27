package iz.mkao.mirasalon.presentation.bookings

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.AdminAppointment
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStatus
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.presentation.components.DateFilter

data class BookingsUiState(
    val userName: String? = null,
    val userAvatar: String? = null,
    val bookings: List<AdminAppointment> = emptyList(),
    val specialists: List<Specialist> = emptyList(),
    val selectedStatus: AdminAppointmentStatus? = null,
    val selectedSpecialistId: String? = null,
    val searchQuery: String = "",
    val dateFilter: DateFilter = DateFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventSink: (BookingsEvent) -> Unit = {}
) : CircuitUiState

/** Circuit UI events for the bookings admin screen. */
sealed interface BookingsEvent : CircuitUiEvent {
    data class StatusFilterChanged(val status: AdminAppointmentStatus?) : BookingsEvent
    data class SpecialistFilterChanged(val specialistId: String?) : BookingsEvent
    data class Search(val query: String) : BookingsEvent
    data class DateFilterChanged(val filter: DateFilter) : BookingsEvent
    data class UpdateBookingStatus(val id: String, val status: AdminAppointmentStatus) : BookingsEvent
    data class DeleteBooking(val id: String) : BookingsEvent
    data object Refresh : BookingsEvent
}
