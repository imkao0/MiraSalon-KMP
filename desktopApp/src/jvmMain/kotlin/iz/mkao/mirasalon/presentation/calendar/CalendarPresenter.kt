package iz.mkao.mirasalon.presentation.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.model.AdminAppointment
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStatus
import iz.mkao.mirasalon.core.domain.model.CreateAppointment
import iz.mkao.mirasalon.core.domain.model.CustomerSummary
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceFilter
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.AdminSalonRepository
import iz.mkao.mirasalon.core.domain.repository.BookingsRepository
import iz.mkao.mirasalon.core.domain.repository.CustomerRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceRepository
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.data.local.TokenManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class CalendarPresenter(
    private val specialistRepository: SpecialistRepository,
    private val bookingsRepository: BookingsRepository,
    private val customerRepository: CustomerRepository,
    private val serviceRepository: ServiceRepository,
    private val salonRepository: AdminSalonRepository,
    private val tokenManager: TokenManager
) : Presenter<CalendarUiState> {

    @Composable
    override fun present(): CalendarUiState {
        var selectedDate by remember {
            mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
        }
        var specialists by remember { mutableStateOf(emptyList<Specialist>()) }
        var appointments by remember { mutableStateOf(emptyList<AdminAppointment>()) }
        var customers by remember { mutableStateOf(emptyList<CustomerSummary>()) }
        var services by remember { mutableStateOf(emptyList<Service>()) }
        var isLoading by remember { mutableStateOf(false) }
        var selectedService by remember { mutableStateOf("All Services") }
        var selectedEmployment by remember { mutableStateOf("All Staff") }
        var selectedStatus by remember { mutableStateOf<AdminAppointmentStatus?>(null) }
        var statusCounts by remember { mutableStateOf(emptyMap<AdminAppointmentStatus, Int>()) }
        var searchQuery by remember { mutableStateOf("") }
        var activeTab by remember { mutableStateOf(0) }
        var startHour by remember { mutableStateOf(8) }
        var endHour by remember { mutableStateOf(17) }
        var timezoneId by remember { mutableStateOf("UTC") }
        
        val salonTimeZone = remember(timezoneId) {
            try { TimeZone.of(timezoneId) } catch (_: Exception) { TimeZone.currentSystemDefault() }
        }

        var selectedAppointment by remember { mutableStateOf<AdminAppointment?>(null) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showBookingDialog by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        var loadJob by remember { mutableStateOf<Job?>(null) }

        var cachedSpecialists by remember { mutableStateOf(emptyList<Specialist>()) }
        var cachedCustomers by remember { mutableStateOf(emptyList<CustomerSummary>()) }
        var cachedServices by remember { mutableStateOf(emptyList<Service>()) }
        var cachedMonthlyBookings by remember { mutableStateOf(emptyList<AdminAppointment>()) }
        var currentFetchedMonth by remember { mutableStateOf<Month?>(null) }

        suspend fun fetchReferenceData(forceRefresh: Boolean) = coroutineScope {
            if (cachedSpecialists.isEmpty() || forceRefresh) {
                val specsDeferred = async { specialistRepository.getSpecialists() }
                val custsDeferred = async { customerRepository.getAll() }
                val servsDeferred = async { serviceRepository.getServices(ServiceFilter()) }
                val salonDeferred = async { salonRepository.getManagementInfo() }

                val specsResult = specsDeferred.await()
                val custsResult = custsDeferred.await()
                val servsResult = servsDeferred.await()
                val salonResult = salonDeferred.await()

                if (specsResult is Outcome.Success) {
                    cachedSpecialists = specsResult.data.filter { it.isActive }
                }
                if (custsResult is Outcome.Success) cachedCustomers = custsResult.data
                if (servsResult is Outcome.Success) cachedServices = servsResult.data
                
                if (salonResult is Outcome.Success) {
                    val salon = salonResult.data.firstOrNull()
                    salon?.let {
                        startHour = it.openTime?.split(":")?.get(0)?.toIntOrNull() ?: 8
                        endHour = it.closeTime?.split(":")?.get(0)?.toIntOrNull() ?: 17
                        timezoneId = it.timezoneId ?: "UTC"
                    }
                }
            }
        }

        suspend fun fetchMonthlyBookings(date: LocalDate) {
            val rangeStart = LocalDate(date.year, date.month, 1)
            val nextMonth = if (date.month == Month.DECEMBER) Month.JANUARY else Month.entries[date.month.ordinal + 1]
            val nextYear = if (date.month == Month.DECEMBER) date.year + 1 else date.year
            val rangeEnd = LocalDate(nextYear, nextMonth, 1).minus(1, DateTimeUnit.DAY)

            val result = bookingsRepository.getAll(
                dateFrom = rangeStart.atStartOfDayIn(salonTimeZone).toEpochMilliseconds(),
                dateTo = rangeEnd.plus(1, DateTimeUnit.DAY).atStartOfDayIn(salonTimeZone).toEpochMilliseconds()
            )
            if (result is Outcome.Success) cachedMonthlyBookings = result.data
        }

        fun filterLocally(
            allBookings: List<AdminAppointment>,
            date: LocalDate,
            service: String,
            specialistName: String,
            status: AdminAppointmentStatus?,
            query: String,
            tab: Int
        ): List<AdminAppointment> {
            return allBookings.filter { app ->
                val instant = Instant.fromEpochMilliseconds(app.dateTime)
                val matchesDate = when (tab) {

                    1 -> {
                        val appDate = instant.toLocalDateTime(salonTimeZone).date
                        val weekStart = date.minus(date.dayOfWeek.ordinal, DateTimeUnit.DAY)
                        val weekEnd = weekStart.plus(7, DateTimeUnit.DAY)
                        appDate >= weekStart && appDate < weekEnd
                    }
                    else -> {
                        val appDate = instant.toLocalDateTime(salonTimeZone).date
                        appDate.year == date.year && appDate.month == date.month
                    }
                }
                val matchesService = service == "All Services" || app.serviceNames.any { it == service }
                val matchesSpecialist = specialistName == "All Staff" || app.specialistName.trim().equals(specialistName.trim(), ignoreCase = true)
                val matchesStatus = status == null || app.status == status
                val matchesQuery = query.isBlank() || 
                    app.customerName.contains(query, ignoreCase = true) ||
                    app.specialistName.contains(query, ignoreCase = true) ||
                    app.serviceNames.any { it.contains(query, ignoreCase = true) }
                
                matchesDate && matchesService && matchesSpecialist && matchesStatus && matchesQuery
            }
        }

        fun loadData(forceRefresh: Boolean) {
            loadJob?.cancel()
            loadJob = scope.launch {
                isLoading = true
                try {
                    val monthChanged = currentFetchedMonth != selectedDate.month || forceRefresh
                    fetchReferenceData(forceRefresh)
                    if (monthChanged) {
                        fetchMonthlyBookings(selectedDate)
                        currentFetchedMonth = selectedDate.month
                    }


                    val relevantForCounts = cachedMonthlyBookings.filter { app ->
                        val timeMillis = app.dateTime
                        when (activeTab) {
        
                            1 -> {
                                val appDate = Instant.fromEpochMilliseconds(timeMillis).toLocalDateTime(salonTimeZone).date
                                val weekStart = selectedDate.minus(selectedDate.dayOfWeek.ordinal, DateTimeUnit.DAY)
                                val weekEnd = weekStart.plus(7, DateTimeUnit.DAY)
                                appDate >= weekStart && appDate < weekEnd
                            }
                            else -> {
                                val appDate = Instant.fromEpochMilliseconds(timeMillis).toLocalDateTime(salonTimeZone).date
                                appDate.year == selectedDate.year && appDate.month == selectedDate.month
                            }
                        }
                    }
                    
                    statusCounts = relevantForCounts.groupBy { it.status }.mapValues { it.value.size }

                    specialists = if (selectedService == "All Services") {
                        cachedSpecialists
                    } else {
                        cachedSpecialists.filter { specialist ->
                            specialist.services.any { it.name == selectedService }
                        }
                    }
                    customers = cachedCustomers
                    services = cachedServices
                    appointments = filterLocally(cachedMonthlyBookings, selectedDate, selectedService, selectedEmployment, selectedStatus, searchQuery, activeTab)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
                isLoading = false
            }
        }

        LaunchedEffect(Unit) { loadData(forceRefresh = false) }

        return CalendarUiState(
            selectedDate = selectedDate,
            specialists = specialists,
            appointments = appointments,
            customers = customers,
            services = services,
            isLoading = isLoading,
            selectedService = selectedService,
            selectedEmployment = selectedEmployment,
            selectedStatus = selectedStatus,
            statusCounts = statusCounts,
            activeTab = activeTab,
            startHour = startHour,
            endHour = endHour,
            timezoneId = timezoneId,
            selectedAppointment = selectedAppointment,
            showDatePicker = showDatePicker,
            showBookingDialog = showBookingDialog
        ) { event ->
            when (event) {
                is CalendarEvent.DateSelected -> {
                    val currentMonth = selectedDate.month
                    selectedDate = event.date
                    showDatePicker = false
                    loadData(forceRefresh = event.date.month != currentMonth)
                }
                is CalendarEvent.ServiceSelected -> {
                    selectedService = event.service
                    loadData(forceRefresh = false)
                }
                is CalendarEvent.EmploymentSelected -> {
                    selectedEmployment = event.employment
                    loadData(forceRefresh = false)
                }
                is CalendarEvent.StatusFilterChanged -> {
                    selectedStatus = event.status
                    loadData(forceRefresh = false)
                }
                is CalendarEvent.TabSelected -> {
                    activeTab = event.tab
                    loadData(forceRefresh = false)
                }
                is CalendarEvent.CreateBooking -> scope.launch {
                    isLoading = true
                    try {
                        val localTime = LocalTime.parse(event.time)
                        val localDateTime = LocalDateTime(event.date, localTime)
                        val instant = localDateTime.toInstant(salonTimeZone)
                        val request = CreateAppointment(
                            salonId = "salon-main-001",
                            userId = event.customerId,
                            specialistId = event.specialistId,
                            dateTime = instant.toEpochMilliseconds(),
                            serviceIds = event.serviceIds
                        )
                        when (bookingsRepository.create(request)) {
                            is Outcome.Success -> {
                                showBookingDialog = false
                                isLoading = false
                                loadData(forceRefresh = true)
                            }
                            else -> isLoading = false
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        isLoading = false
                    }
                }
                is CalendarEvent.Search -> {
                    searchQuery = event.query
                    appointments = filterLocally(cachedMonthlyBookings, selectedDate, selectedService, selectedEmployment, selectedStatus, searchQuery, activeTab)
                }
                is CalendarEvent.ShowDatePicker -> showDatePicker = event.show
                is CalendarEvent.SelectAppointment -> selectedAppointment = event.appointment
                CalendarEvent.NextDay -> {
                    val next = selectedDate.plus(DatePeriod(days = 1))
                    val currentMonth = selectedDate.month
                    selectedDate = next
                    loadData(forceRefresh = next.month != currentMonth)
                }
                CalendarEvent.PreviousDay -> {
                    val prev = selectedDate.minus(DatePeriod(days = 1))
                    val currentMonth = selectedDate.month
                    selectedDate = prev
                    loadData(forceRefresh = prev.month != currentMonth)
                }
                is CalendarEvent.ShowBookingDialog -> showBookingDialog = event.show
                CalendarEvent.Refresh -> loadData(forceRefresh = true)
            }
        }
    }
}
