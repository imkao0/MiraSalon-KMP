package iz.mkao.mirasalon.feature.booking.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.core.designsystem.components.MiraEmptyState
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.RectangularSwitch
import iz.mkao.mirasalon.core.designsystem.components.ReviewBottomSheet
import iz.mkao.mirasalon.core.designsystem.theme.RadiusSmall
import iz.mkao.mirasalon.feature.booking.domain.model.BookingStatus
import iz.mkao.mirasalon.feature.booking.domain.model.ConfirmedBooking
import iz.mkao.mirasalon.feature.booking.presentation.circuit.MyBookingsEvent
import iz.mkao.mirasalon.feature.booking.presentation.circuit.MyBookingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsUi(
    state: MyBookingsState,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            state.eventSink(MyBookingsEvent.DismissError)
        }
    }

    if (state.showReviewSheet && state.onReviewSubmit != null) {
        ReviewBottomSheet(
            onDismiss = { state.eventSink(MyBookingsEvent.DismissReviewSheet) },
            onReviewSubmit = state.onReviewSubmit
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BookingsTopBar(
                selectedStatus = state.selectedStatus,
                onBack = { state.eventSink(MyBookingsEvent.Back) },
                onTabSelected = { status -> state.eventSink(MyBookingsEvent.TabSelected(status)) }
            )
        }
    ) { padding ->
        val bookings = state.filteredBookings

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (bookings.isEmpty() && !state.isLoading) {
                MiraEmptyState(
                    message = "No ${state.selectedStatus.name.lowercase()} bookings found",
                    description = "You don't have any ${state.selectedStatus.name.lowercase()} appointments yet.",
                    icon = Icons.Outlined.CalendarToday,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (bookings.isEmpty() && state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(bookings.distinctBy { it.id }, key = { it.id }) { booking ->
                        BookingCard(
                            booking = booking,
                            currentTimeMillis = state.currentTimeMillis,
                            onReminderToggled = { enabled ->
                                state.eventSink(MyBookingsEvent.ReminderToggled(booking.id, enabled))
                            },
                            onPrimaryActionClicked = {
                                when (state.selectedStatus) {
                                    BookingStatus.Confirmed -> state.eventSink(MyBookingsEvent.EReceiptClicked(booking.id))
                                    BookingStatus.Completed -> state.eventSink(MyBookingsEvent.AddReviewClicked(booking.id))
                                    BookingStatus.Cancelled -> state.eventSink(MyBookingsEvent.RebookClicked(booking))
                                }
                            },
                            onSecondaryActionClicked = {
                                when (state.selectedStatus) {
                                    BookingStatus.Confirmed -> state.eventSink(MyBookingsEvent.CancelClicked(booking.id))
                                    BookingStatus.Completed -> state.eventSink(MyBookingsEvent.RebookClicked(booking))
                                    BookingStatus.Cancelled -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingsTopBar(
    selectedStatus: BookingStatus,
    onBack: () -> Unit,
    onTabSelected: (BookingStatus) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        MiraTopAppBar(
            title = "Appointments",
            onBackClick = onBack
        )

        TabRow(
            selectedTabIndex = selectedStatus.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            BookingStatus.entries.forEach { status ->
                val isSelected = status == selectedStatus
                val label = when (status) {
                    BookingStatus.Confirmed -> "Upcoming"
                    BookingStatus.Completed -> "Completed"
                    BookingStatus.Cancelled -> "Cancelled"
                }
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(status) },
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: ConfirmedBooking,
    currentTimeMillis: Long,
    onReminderToggled: (Boolean) -> Unit,
    onPrimaryActionClicked: () -> Unit,
    onSecondaryActionClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(2.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = DateUtils.formatUpcomingDate(booking.dateTime, currentTimeMillis),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (booking.createdAt > 0) {
                    Text(
                        text = DateUtils.formatBookedDate(booking.createdAt, currentTimeMillis),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
            if (booking.status == BookingStatus.Confirmed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Remind me",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    RectangularSwitch(
                        checked = booking.reminderEnabled,
                        onCheckedChange = onReminderToggled
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = booking.salonImageUrl ?: booking.serviceImageUrl,
                contentDescription = booking.salonName,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = booking.services.firstOrNull()?.name ?: "Service",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = booking.salonName,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Specialist: ${booking.specialistName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Service ID: # ${booking.id.take(8).uppercase()}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (booking.status) {
            BookingStatus.Confirmed -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSecondaryActionClicked,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onPrimaryActionClicked,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("E-Receipt", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            BookingStatus.Completed -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSecondaryActionClicked,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Re - book", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onPrimaryActionClicked,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (booking.isReviewed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        contentColor = if (booking.isReviewed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !booking.isReviewed
                ) {
                    Text(
                        text = if (booking.isReviewed) "Completed" else "Add Review",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            BookingStatus.Cancelled -> Button(
                onClick = onPrimaryActionClicked,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(RadiusSmall),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Re - Book", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
