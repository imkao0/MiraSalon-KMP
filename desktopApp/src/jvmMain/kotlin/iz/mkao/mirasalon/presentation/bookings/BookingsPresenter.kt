package iz.mkao.mirasalon.presentation.bookings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStatus
import iz.mkao.mirasalon.core.domain.model.AdminAppointment
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.BookingsRepository
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.data.local.TokenManager
import iz.mkao.mirasalon.presentation.components.DateFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class BookingsPresenter(
    private val repository: BookingsRepository,
    private val specialistRepository: SpecialistRepository,
    private val tokenManager: TokenManager,
    private val realtimeGateway: RealtimeGateway
) : Presenter<BookingsUiState> {

    @Composable
    override fun present(): BookingsUiState {
        val session by tokenManager.session.collectAsState()
        val userName = session.name
        val userAvatar = session.avatarUrl

        var bookings by remember { mutableStateOf(emptyList<AdminAppointment>()) }
        var specialists by remember { mutableStateOf(emptyList<Specialist>()) }
        var selectedStatus by remember { mutableStateOf<AdminAppointmentStatus?>(null) }
        var selectedSpecialistId by remember { mutableStateOf<String?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        var dateFilter by remember { mutableStateOf(DateFilter.ALL) }
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        
        val scope = rememberCoroutineScope()

        fun loadData(showLoading: Boolean = true) {
            scope.launch {
                if (showLoading) isLoading = true
                error = null

                val now = System.currentTimeMillis()
                val dateFrom = when (dateFilter) {
                    DateFilter.TODAY -> {
                        val calendar = java.util.Calendar.getInstance()
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        calendar.set(java.util.Calendar.MINUTE, 0)
                        calendar.set(java.util.Calendar.SECOND, 0)
                        calendar.set(java.util.Calendar.MILLISECOND, 0)
                        calendar.timeInMillis
                    }
                    DateFilter.SEVEN_DAYS -> now - (7L * 24 * 60 * 60 * 1000)
                    DateFilter.THIRTY_DAYS -> now - (30L * 24 * 60 * 60 * 1000)
                    DateFilter.ALL -> null
                }
                val dateTo = if (dateFrom != null) now else null

                try {
                    coroutineScope {
                        val bookingsDeferred = async {
                            repository.getAll(
                                status = selectedStatus,
                                query = searchQuery.ifBlank { null },
                                dateFrom = dateFrom,
                                dateTo = dateTo
                            )
                        }
                        val specialistsDeferred = async {
                            specialistRepository.getSpecialists()
                        }

                        val bookingsResult = bookingsDeferred.await()
                        val specialistsResult = specialistsDeferred.await()

                        when (bookingsResult) {
                            is Outcome.Success -> {
                                var list = bookingsResult.data
                                if (selectedSpecialistId != null) {
                                    list = list.filter { it.specialistId == selectedSpecialistId }
                                }
                                bookings = list
                            }
                            is Outcome.Error -> error = "Bookings: ${bookingsResult.failure}"
                            else -> Unit
                        }

                        when (specialistsResult) {
                            is Outcome.Success -> specialists = specialistsResult.data.filter { it.isActive }
                            is Outcome.Error -> error = error ?: "Specialists error"
                            else -> Unit
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    error = "System error: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }

        LaunchedEffect(selectedStatus, selectedSpecialistId, searchQuery, dateFilter) {
            loadData()
        }

        LaunchedEffect(Unit) {
            realtimeGateway.events.collect { event ->
                if (event is DomainEvent.BookingCreated ||
                    event is DomainEvent.BookingUpdated
                ) {
                    loadData()
                }
            }
        }

        return BookingsUiState(
            bookings = bookings,
            specialists = specialists,
            userName = userName,
            userAvatar = userAvatar,
            selectedStatus = selectedStatus,
            selectedSpecialistId = selectedSpecialistId,
            searchQuery = searchQuery,
            dateFilter = dateFilter,
            isLoading = isLoading,
            error = error
        ) { event ->
            when (event) {
                is BookingsEvent.StatusFilterChanged -> selectedStatus = event.status
                is BookingsEvent.SpecialistFilterChanged -> selectedSpecialistId = event.specialistId
                is BookingsEvent.Search -> searchQuery = event.query
                is BookingsEvent.DateFilterChanged -> dateFilter = event.filter
                is BookingsEvent.UpdateBookingStatus -> {

                    bookings = bookings.map { if (it.id == event.id) it.copy(status = event.status) else it }
                    
                    scope.launch {
                        try {
                            when (val result = repository.updateStatus(event.id, event.status)) {
                                is Outcome.Success -> loadData(showLoading = false)
                                else -> {
                                    // Handle error or loading if necessary
                                    loadData(showLoading = false)
                                }
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            loadData(showLoading = false)
                        }
                    }
                }
                is BookingsEvent.DeleteBooking -> {

                    bookings = bookings.filter { it.id != event.id }
                    
                    scope.launch {
                        try {
                            when (repository.delete(event.id)) {
                                is Outcome.Success -> loadData(showLoading = false)
                                else -> loadData(showLoading = false)
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            loadData(showLoading = false)
                        }
                    }
                }
                BookingsEvent.Refresh -> loadData(showLoading = true)
            }
        }
    }
}
