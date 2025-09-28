package iz.mkao.mirasalon.presentation.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.plusMonths
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.CalendarRowHeight
import iz.mkao.mirasalon.core.designsystem.theme.CalendarSlotWidth
import iz.mkao.mirasalon.core.designsystem.theme.CalendarTimeHeaderHeight
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.RadiusMedium
import iz.mkao.mirasalon.core.domain.model.AdminAppointment
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStatus
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.LocalDesktopNavigate
import iz.mkao.mirasalon.presentation.LocalProfileClick
import iz.mkao.mirasalon.presentation.LocalSidebarExpanded
import iz.mkao.mirasalon.presentation.LocalToggleSidebar
import iz.mkao.mirasalon.presentation.calendar.components.AppointmentBlock
import iz.mkao.mirasalon.presentation.calendar.components.CalendarFilterBar
import iz.mkao.mirasalon.presentation.calendar.components.SpecialistRowHeader
import iz.mkao.mirasalon.presentation.dashboard.components.Sidebar
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth
import kotlin.time.Clock

@Composable
fun CalendarScreenUi(
    state: CalendarUiState,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    isSidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val uiState = state
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }


    val daysOfWeek = remember { daysOfWeek() }
    val calendarState = rememberCalendarState(
        startMonth = today.yearMonth.minusMonths(12),
        endMonth = today.yearMonth.plusMonths(12),
        firstVisibleMonth = uiState.selectedDate.yearMonth,
        firstDayOfWeek = daysOfWeek.first()
    )


    LaunchedEffect(uiState.selectedDate) {
        if (calendarState.firstVisibleMonth.yearMonth != uiState.selectedDate.yearMonth) {
            calendarState.animateScrollToMonth(uiState.selectedDate.yearMonth)
        }
    }


    LaunchedEffect(calendarState.firstVisibleMonth) {
        if (uiState.activeTab == 0) {
            val visibleMonth = calendarState.firstVisibleMonth.yearMonth
            if (visibleMonth != uiState.selectedDate.yearMonth) {

                state.eventSink(CalendarEvent.DateSelected(LocalDate(visibleMonth.year, visibleMonth.month, 1)))
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Sidebar(
            isExpanded = isSidebarExpanded,
            onToggle = onToggleSidebar,
            selectedRoute = "Calendar",
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxHeight().width(if (isSidebarExpanded) 280.dp else 80.dp)
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {

            Surface(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MiraBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Salon Calendar",
                        color = MiraTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.width(32.dp))


                    Surface(
                        modifier = Modifier.width(300.dp).height(40.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, MiraBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp), tint = MiraTextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = { state.eventSink(CalendarEvent.Search(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 14.sp, color = MiraTextPrimary),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text("Search calendar...", fontSize = 14.sp, color = MiraTextSecondary)
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { state.eventSink(CalendarEvent.PreviousDay) }) {
                            Icon(Icons.Outlined.ChevronLeft, null)
                        }
                        TextButton(onClick = { state.eventSink(CalendarEvent.ShowDatePicker(true)) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                val headerText = when (uiState.activeTab) {
                                    2 -> {
                                        val date = uiState.selectedDate
                                        "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}, ${date.year}"
                                    }
                                    1 -> {
                                        val weekStart = uiState.selectedDate.minus(uiState.selectedDate.dayOfWeek.ordinal, DateTimeUnit.DAY)
                                        val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
                                        "${weekStart.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${weekStart.dayOfMonth} - ${weekEnd.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${weekEnd.dayOfMonth}, ${weekEnd.year}"
                                    }
                                    else -> {
                                        val visibleMonth = calendarState.firstVisibleMonth.yearMonth
                                        "${visibleMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${visibleMonth.year}"
                                    }
                                }

                                Text(
                                    headerText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MiraTextPrimary
                                )
                            }
                        }
                        IconButton(onClick = { state.eventSink(CalendarEvent.NextDay) }) {
                            Icon(Icons.Outlined.ChevronRight, null)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { state.eventSink(CalendarEvent.ShowBookingDialog(true)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MiraCoral),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Icon(Icons.Outlined.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Booking")
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onProfileClick() },
                        shape = CircleShape,
                        color = Color.LightGray
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Person,
                                null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }


            CalendarFilterBar(
                selectedService = uiState.selectedService,
                onServiceSelected = { state.eventSink(CalendarEvent.ServiceSelected(it)) },
                selectedEmployment = uiState.selectedEmployment,
                onEmploymentSelected = { state.eventSink(CalendarEvent.EmploymentSelected(it)) },
                selectedStatus = uiState.selectedStatus,
                onStatusSelected = { state.eventSink(CalendarEvent.StatusFilterChanged(it)) },
                statusCounts = uiState.statusCounts,
                activeTab = uiState.activeTab,
                onTabChange = { state.eventSink(CalendarEvent.TabSelected(it)) }
            )


            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState.activeTab) {
                    2 -> DayTimelineView(
                        specialists = uiState.specialists,
                        appointments = uiState.appointments,
                        onAppointmentClick = { state.eventSink(CalendarEvent.SelectAppointment(it)) },
                        isLoading = uiState.isLoading,
                        startHour = uiState.startHour,
                        endHour = uiState.endHour,
                        timezoneId = uiState.timezoneId
                    )
                    1 -> WeekTimelineView(
                        selectedDate = uiState.selectedDate,
                        specialists = uiState.specialists,
                        appointments = uiState.appointments,
                        onAppointmentClick = { state.eventSink(CalendarEvent.SelectAppointment(it)) },
                        timezoneId = uiState.timezoneId
                    )
                    else -> CalendarMonthView(
                        calendarState = calendarState,
                        today = today,
                        selectedDate = uiState.selectedDate,
                        appointments = uiState.appointments,
                        timezoneId = uiState.timezoneId,
                    ) {
                        state.eventSink(CalendarEvent.DateSelected(it))
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        ShimmerLoading()
                    }
                }
            }
        }
    }

    uiState.selectedAppointment?.let { appointment ->
        AppointmentDetailsSheet(
            appointment = appointment,
            onDismiss = { state.eventSink(CalendarEvent.SelectAppointment(null)) }
        )
    }

    if (uiState.showDatePicker) {
        CalendarPickerDialog(
            selectedDate = uiState.selectedDate,
            onDateSelected = { state.eventSink(CalendarEvent.DateSelected(it)) },
            onDismiss = { state.eventSink(CalendarEvent.ShowDatePicker(false)) }
        )
    }

    if (uiState.showBookingDialog) {
        BookingDialog(
            initialDate = uiState.selectedDate,
            specialists = uiState.specialists,
            services = uiState.services,
            customers = uiState.customers,
            onSave = { customerId, specialistId, serviceIds, date, time ->
                state.eventSink(CalendarEvent.CreateBooking(customerId, specialistId, serviceIds, date, time))
            },
            onDismiss = { state.eventSink(CalendarEvent.ShowBookingDialog(false)) }
        )
    }
}

@Composable
fun WeekTimelineView(
    selectedDate: LocalDate,
    specialists: List<Specialist>,
    appointments: List<AdminAppointment>,
    onAppointmentClick: (AdminAppointment) -> Unit,
    modifier: Modifier = Modifier,
    timezoneId: String = "UTC"
) {
    val salonTimeZone = remember(timezoneId) {
        try { TimeZone.of(timezoneId) } catch (_: Exception) { TimeZone.currentSystemDefault() }
    }
    val weekStart = remember(selectedDate) {
        selectedDate.minus(selectedDate.dayOfWeek.ordinal, DateTimeUnit.DAY)
    }
    val weekDays = remember(weekStart) {
        (0..6).map { weekStart.plus(it, DateTimeUnit.DAY) }
    }

    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .horizontalScroll(scrollState)
    ) {
        weekDays.forEach { date ->
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .border(BorderStroke(0.5.dp, MiraBorder))
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CalendarTimeHeaderHeight)
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${date.dayOfWeek.name.take(3)}, ${date.month.name.take(3)} ${date.dayOfMonth}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiraTextPrimary
                    )
                }


                val dayAppointments = appointments.filter {
                    Instant.fromEpochMilliseconds(it.dateTime).toLocalDateTime(salonTimeZone).date == date
                }.sortedBy { it.dateTime }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (dayAppointments.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No bookings", fontSize = 12.sp, color = MiraTextSecondary)
                        }
                    } else {
                        dayAppointments.forEach { appt ->
                            AppointmentBlock(
                                appt = appt,
                                onClick = { onAppointmentClick(appt) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayTimelineView(
    specialists: List<Specialist>,
    appointments: List<AdminAppointment>,
    onAppointmentClick: (AdminAppointment) -> Unit,
    isLoading: Boolean = false,
    onStatusUpdate: ((AdminAppointment, AdminAppointmentStatus) -> Unit)? = null,
    modifier: Modifier = Modifier,
    showSpecialistHeaders: Boolean = true,
    slotWidth: Dp = CalendarSlotWidth,
    rowHeight: Dp = CalendarRowHeight,
    startHour: Int = 8,
    endHour: Int = 17,
    timezoneId: String = "UTC"
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val totalSlots = (endHour - startHour + 1) * 4
    val gridWidth = slotWidth * totalSlots

    Column(modifier = modifier.background(Color.White)) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CalendarTimeHeaderHeight)
                .background(Color(0xFFF5F5F5))
        ) {
            if (showSpecialistHeaders) {
                Box(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .border(BorderStroke(0.5.dp, MiraBorder))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState)
            ) {
                (startHour..endHour).forEach { hour ->
                    repeat(4) { min ->
                        val timeStr = "${hour.toString().padStart(2, '0')}:${(min * 15).toString().padStart(2, '0')}"
                        TimeSlotHeader(timeStr, slotWidth = slotWidth)
                    }
                }
            }
        }

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (showSpecialistHeaders) {
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .verticalScroll(verticalScrollState)
                        .border(BorderStroke(0.5.dp, MiraBorder))
                ) {
                    specialists.forEach { specialist ->
                        SpecialistRowHeader(specialist)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState)
                    .verticalScroll(verticalScrollState)
            ) {
                Box(modifier = Modifier.width(gridWidth).fillMaxHeight()) {
                    Column {
                        specialists.forEach { specialist ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight)
                                    .border(BorderStroke(0.5.dp, MiraBorder))
                            ) {
                                (startHour..endHour).forEach { hour ->
                                    repeat(4) { min ->
                                        val slotIndex = (hour - startHour) * 4 + min
                                        val xOffset = slotIndex * slotWidth.value
                                        Box(
                                            modifier = Modifier
                                                .offset(x = xOffset.dp)
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(MiraBorder)
                                        )
                                    }
                                }

                                val specialistAppointments = appointments
                                    .filter { it.specialistId == specialist.id }

                                if (specialistAppointments.isEmpty() && appointments.isNotEmpty()) {

                                    val nameBased = appointments.filter { it.specialistName.trim().equals(specialist.name.trim(), ignoreCase = true) }
                                    nameBased.forEach { appointment ->
                                        InteractiveAppointmentBlock(
                                            appointment = appointment,
                                            onClick = { onAppointmentClick(appointment) },
                                            onStatusUpdate = onStatusUpdate,
                                            slotWidth = slotWidth,
                                            startHour = startHour,
                                            timezoneId = timezoneId,
                                        )
                                    }
                                } else {
                                    specialistAppointments.forEach { appointment ->
                                        InteractiveAppointmentBlock(
                                            appointment = appointment,
                                            onClick = { onAppointmentClick(appointment) },
                                            onStatusUpdate = onStatusUpdate,
                                            slotWidth = slotWidth,
                                            startHour = startHour,
                                            timezoneId = timezoneId,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    

                    val matchedIds = specialists.map { it.id }.toSet()
                    val unmatchedAppointments = appointments.filter { 
                        it.specialistId !in matchedIds && 
                        specialists.none { s -> it.specialistName.trim().equals(s.name.trim(), ignoreCase = true) } 
                    }
                    
                    if (unmatchedAppointments.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .offset(y = (specialists.size * rowHeight.value).dp)
                                .border(BorderStroke(0.5.dp, MiraBorder))
                        ) {
                            unmatchedAppointments.forEach { appointment ->
                                InteractiveAppointmentBlock(
                                    appointment = appointment,
                                    onClick = { onAppointmentClick(appointment) },
                                    onStatusUpdate = onStatusUpdate,
                                    slotWidth = slotWidth,
                                    startHour = startHour,
                                    timezoneId = timezoneId,
                                )
                            }
                        }
                    }

                    CurrentTimeIndicator(slotWidth = slotWidth, startHour = startHour, endHour = endHour, timezoneId = timezoneId)
                }
            }
        }
        

        if (appointments.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No appointments found for this day", color = MiraTextSecondary)
            }
        }
    }
}

@Composable
fun CalendarMonthView(
    calendarState: CalendarState,
    today: LocalDate,
    selectedDate: LocalDate,
    appointments: List<AdminAppointment>,
    timezoneId: String = "UTC",
    onDateSelected: (LocalDate) -> Unit,
) {
    val salonTimeZone = remember(timezoneId) {
        try { TimeZone.of(timezoneId) } catch (_: Exception) { TimeZone.currentSystemDefault() }
    }
    val daysOfWeek = remember { daysOfWeek() }

    val appointmentsByDate = remember(appointments) {
        appointments.groupBy {
            kotlin.time.Instant.fromEpochMilliseconds(it.dateTime).toLocalDateTime(salonTimeZone).date
        }
    }

    val currentMonth = calendarState.firstVisibleMonth.yearMonth
    val firstDayOfMonth = LocalDate(currentMonth.year, currentMonth.month, 1)
    val lastDayOfMonth = firstDayOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.ordinal
    
    val daysInMonth = remember(currentMonth) {
        val days = mutableListOf<LocalDate>()
        // Add padding days from previous month
        val paddingStart = firstDayOfWeek
        for (i in paddingStart downTo 1) {
            days.add(firstDayOfMonth.minus(i, DateTimeUnit.DAY))
        }
        // Add all days of current month
        for (day in 1..lastDayOfMonth.day) {
            days.add(LocalDate(currentMonth.year, currentMonth.month, day))
        }
        // Add padding days from next month to complete the grid
        val totalDays = days.size
        val paddingEnd = (7 - (totalDays % 7)) % 7
        for (i in 1..paddingEnd) {
            days.add(lastDayOfMonth.plus(i, DateTimeUnit.DAY))
        }
        days
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        MonthHeader(daysOfWeek = daysOfWeek)
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(0.dp)
        ) {
            items(daysInMonth.size) { index ->
                val date = daysInMonth[index]
                val isCurrentMonth = date.year == currentMonth.year && date.month == currentMonth.month
                val dayAppointments = appointmentsByDate[date] ?: emptyList()
                MonthDayContent(
                    day = CalendarDay(
                        date = date,
                        position = if (isCurrentMonth) DayPosition.MonthDate else DayPosition.OutDate
                    ),
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    appointments = dayAppointments,
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
fun MonthHeader(daysOfWeek: List<DayOfWeek>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(vertical = 12.dp)
            .border(BorderStroke(0.5.dp, MiraBorder))
    ) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MiraTextSecondary
            )
        }
    }
}

@Composable
fun MonthDayContent(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    appointments: List<AdminAppointment>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(BorderStroke(0.5.dp, MiraBorder.copy(alpha = 0.5f)))
            .background(
                when {
                    isSelected -> MiraCoral.copy(alpha = 0.1f)
                    day.position == DayPosition.MonthDate -> Color.White
                    else -> Color.White.copy(alpha = 0.5f)
                }
            )
            .clickable(enabled = day.position == DayPosition.MonthDate, onClick = onClick)
            .padding(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isToday && !isSelected -> MiraCoral.copy(alpha = 0.1f)
                        else -> Color.Transparent
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = day.date.dayOfMonth.toString(),
                            color = when {
                                isSelected -> MiraCoral
                                isToday -> MiraCoral
                                day.position == DayPosition.MonthDate -> MiraTextPrimary
                                else -> MiraTextSecondary.copy(alpha = 0.5f)
                            },
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MiraCoral)
                    )
                } else if (appointments.isNotEmpty()) {

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MiraCoral.copy(alpha = 0.4f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))


            if (day.position == DayPosition.MonthDate) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    appointments.take(3).forEach { appt ->
                        MonthViewAppointmentItem(appt)
                    }
                    if (appointments.size > 3) {
                        Text(
                            "+${appointments.size - 3} more",
                            fontSize = 9.sp,
                            color = MiraTextSecondary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthViewAppointmentItem(appointment: AdminAppointment) {
    val color = when (appointment.status) {
        AdminAppointmentStatus.Confirmed -> MaterialTheme.colorScheme.primary
        AdminAppointmentStatus.Completed -> MaterialTheme.colorScheme.tertiary
        AdminAppointmentStatus.Cancelled -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = appointment.customerName,
            fontSize = 9.sp,
            color = MiraTextPrimary,
            maxLines = 1,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun InteractiveAppointmentBlock(
    appointment: AdminAppointment,
    onClick: () -> Unit,
    onStatusUpdate: ((AdminAppointment, AdminAppointmentStatus) -> Unit)? = null,
    slotWidth: Dp = CalendarSlotWidth,
    startHour: Int = 8,
    timezoneId: String = "UTC"
) {
    val salonTimeZone = remember(timezoneId) {
        try { TimeZone.of(timezoneId) } catch (_: Exception) { TimeZone.currentSystemDefault() }
    }
    val time = try {
        Instant.fromEpochMilliseconds(appointment.dateTime).toLocalDateTime(salonTimeZone)
    } catch (_: Exception) {
        null
    }

    if (time == null) return

    val hour = time.hour
    val minute = time.minute

    val totalMinutesFromStart = (hour - startHour) * 60 + minute
    val xOffset = (totalMinutesFromStart / 15f) * slotWidth.value
    val width = (appointment.durationMinutes / 15f) * slotWidth.value

    Box(
        modifier = Modifier
            .offset(x = xOffset.dp)
            .width(width.dp)
            .fillMaxHeight()
    ) {
        AppointmentBlock(
            appt = appointment,
            onClick = onClick,
            onStatusUpdate = onStatusUpdate
        )
    }
}

@Composable
fun AppointmentDetailsSheet(
    appointment: AdminAppointment,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(400.dp).padding(16.dp),
            shape = RoundedCornerShape(RadiusMedium),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    "Appointment Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MiraTextPrimary
                )
                Spacer(modifier = Modifier.height(24.dp))

                val (bgColor, borderColor) = when (appointment.status) {
                    AdminAppointmentStatus.Confirmed -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
                    AdminAppointmentStatus.Completed -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
                    AdminAppointmentStatus.Cancelled -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor, RoundedCornerShape(RadiusMedium))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .background(borderColor, RoundedCornerShape(RadiusMedium))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            appointment.customerName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiraTextPrimary
                        )
                        Text(
                            appointment.serviceNames.firstOrNull() ?: "Service",
                            fontSize = 13.sp,
                            color = MiraTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                DetailRow("Specialist", appointment.specialistName)
                DetailRow("Date & Time", appointment.dateTime.toString())
                DetailRow("Duration", "${appointment.durationMinutes} min")
                DetailRow("Total Amount", "$${appointment.totalAmount}")

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = MiraTextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MiraTextPrimary)
    }
}

@Composable
fun TimeSlotHeader(time: String, slotWidth: Dp = CalendarSlotWidth) {
    Box(
        modifier = Modifier.width(slotWidth).fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Text(time, fontSize = 12.sp, color = MiraTextSecondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CurrentTimeIndicator(
    slotWidth: Dp = CalendarSlotWidth,
    startHour: Int = 8,
    endHour: Int = 17,
    timezoneId: String = "UTC"
) {
    val salonTimeZone = remember(timezoneId) {
        try { TimeZone.of(timezoneId) } catch (_: Exception) { TimeZone.currentSystemDefault() }
    }
    val now = Clock.System.now().toLocalDateTime(salonTimeZone)
    if (now.hour in startHour..endHour) {
        val minutesFromStart = (now.hour - startHour) * 60 + now.minute
        val xOffset = (minutesFromStart / 15f) * slotWidth.value

        Box(
            modifier = Modifier
                .offset(x = xOffset.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.error)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
            )
        }
    }
}

@Composable
fun CalendarPickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(350.dp).padding(16.dp),
            shape = RoundedCornerShape(RadiusMedium),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Select Date", fontWeight = FontWeight.Bold)
                Button(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}


class CalendarUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Calendar -> ui<CalendarUiState> { state, modifier ->
            CalendarScreenUi(
                state = state,
                modifier = modifier,
                onNavigate = LocalDesktopNavigate.current,
                isSidebarExpanded = LocalSidebarExpanded.current,
                onToggleSidebar = LocalToggleSidebar.current,
                onProfileClick = LocalProfileClick.current
            )
        }
        else -> null
    }
}
