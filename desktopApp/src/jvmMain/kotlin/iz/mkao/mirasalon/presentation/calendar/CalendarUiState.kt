package iz.mkao.mirasalon.presentation.calendar

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class CalendarUiState(
    val selectedDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val specialists: List<Specialist> = emptyList(),
    val appointments: List<AdminAppointment> = emptyList(),
    val customers: List<CustomerSummary> = emptyList(),
    val services: List<Service> = emptyList(),
    val isLoading: Boolean = false,
    val selectedService: String = "All Services",
    val selectedEmployment: String = "All Staff",
    val selectedStatus: AdminAppointmentStatus? = null,
    val statusCounts: Map<AdminAppointmentStatus, Int> = emptyMap(),
    val activeTab: Int = 0,
    val startHour: Int = 8,
    val endHour: Int = 17,
    val timezoneId: String = "UTC",
    val selectedAppointment: AdminAppointment? = null,
    val searchQuery: String = "",
    val showDatePicker: Boolean = false,
    val showBookingDialog: Boolean = false,
    val eventSink: (CalendarEvent) -> Unit = {}
) : CircuitUiState

/** Circuit UI events for the calendar admin screen. */
sealed interface CalendarEvent : CircuitUiEvent {
    data class Search(val query: String) : CalendarEvent
    data class DateSelected(val date: LocalDate) : CalendarEvent
    data class ServiceSelected(val service: String) : CalendarEvent
    data class EmploymentSelected(val employment: String) : CalendarEvent
    data class StatusFilterChanged(val status: AdminAppointmentStatus?) : CalendarEvent
    data class TabSelected(val tab: Int) : CalendarEvent
    data class CreateBooking(
        val customerId: String,
        val specialistId: String,
        val serviceIds: List<String>,
        val date: LocalDate,
        val time: String
    ) : CalendarEvent
    data class ShowDatePicker(val show: Boolean) : CalendarEvent
    data class SelectAppointment(val appointment: AdminAppointment?) : CalendarEvent
    data object NextDay : CalendarEvent
    data object PreviousDay : CalendarEvent
    data class ShowBookingDialog(val show: Boolean) : CalendarEvent
    data object Refresh : CalendarEvent
}
