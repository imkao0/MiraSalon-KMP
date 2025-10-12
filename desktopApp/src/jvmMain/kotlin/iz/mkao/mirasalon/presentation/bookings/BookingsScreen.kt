package iz.mkao.mirasalon.presentation.bookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.domain.model.AdminAppointment
import iz.mkao.mirasalon.core.domain.model.AdminAppointmentStatus
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.LocalDesktopNavigate
import iz.mkao.mirasalon.presentation.LocalProfileClick
import iz.mkao.mirasalon.presentation.LocalSidebarExpanded
import iz.mkao.mirasalon.presentation.LocalToggleSidebar
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardHeader
import iz.mkao.mirasalon.presentation.dashboard.components.Sidebar
import iz.mkao.mirasalon.core.common.util.ChatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookingsScreenUi(
    state: BookingsUiState,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    isSidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Sidebar(
            isExpanded = isSidebarExpanded,
            onToggle = onToggleSidebar,
            selectedRoute = "Bookings",
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxHeight().width(if (isSidebarExpanded) 280.dp else 80.dp)
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF9F9F9))) {
            Box(modifier = Modifier.padding(horizontal = 40.dp, vertical = 24.dp)) {
                DashboardHeader(
                    title = "Welcome back",
                    userName = state.userName,
                    userAvatar = state.userAvatar,
                    onProfileClick = onProfileClick
                )
            }

            Column(modifier = Modifier.padding(horizontal = 40.dp)) {
                Text(
                    "Bookings",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                BookingTabs(
                    selectedStatus = state.selectedStatus,
                    onStatusChange = { state.eventSink(BookingsEvent.StatusFilterChanged(it)) }
                )

                HorizontalDivider(color = MiraBorder.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isLoading && state.bookings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ShimmerLoading()
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.bookings) { booking ->
                            BookingListItem(
                                booking = booking,
                                onStatusChange = { newStatus -> state.eventSink(BookingsEvent.UpdateBookingStatus(booking.id, newStatus)) },
                                onNavigate = onNavigate
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                            Text(
                                "No more results",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingTabs(
    selectedStatus: AdminAppointmentStatus?,
    onStatusChange: (AdminAppointmentStatus?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val tabs = listOf(
            "Upcoming" to AdminAppointmentStatus.Confirmed,
            "Past" to AdminAppointmentStatus.Completed,
            "Cancelled" to AdminAppointmentStatus.Cancelled
        )

        tabs.forEach { (label, status) ->
            val isSelected = selectedStatus == status
            Surface(
                modifier = Modifier
                    .clickable { onStatusChange(status) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MiraCoral else Color.Transparent,
                border = if (isSelected) null else BorderStroke(1.dp, MiraBorder)
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MiraTextSecondary
                )
            }
        }
    }
}

@Composable
fun BookingListItem(
    booking: AdminAppointment,
    onStatusChange: (AdminAppointmentStatus) -> Unit,
    onNavigate: (String) -> Unit
) {
    val monthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val date = Date(booking.dateTime)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MiraBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Box
            Column(
                modifier = Modifier
                    .size(64.dp)
                    .border(1.dp, MiraBorder, RoundedCornerShape(8.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    monthFormat.format(date).uppercase(),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    dayFormat.format(date),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        booking.serviceNames.firstOrNull() ?: "Service",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StatusBadge(booking.status)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Client: ${booking.customerName}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Booked: ${timeFormat.format(Date(booking.createdAt))}",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfoItem(icon = Icons.Outlined.AccessTime, text = timeFormat.format(date))
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Specialist with small circular avatar/icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFF0F0F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(booking.specialistName, fontSize = 13.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    InfoItem(icon = Icons.Outlined.LocationOn, text = booking.salonName.ifBlank { "Valveta Salon" })
                }
            }

            // Edit Button
            var showMenu by remember { mutableStateOf(false) }
            Box {
                Surface(
                    onClick = { showMenu = true },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MiraBorder),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Edit", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Outlined.ExpandMore, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    }
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Confirmed") },
                        onClick = { onStatusChange(AdminAppointmentStatus.Confirmed); showMenu = false },
                        leadingIcon = {
                            if (booking.status == AdminAppointmentStatus.Confirmed) {
                                Icon(Icons.Outlined.AccessTime, null, tint = MiraCoral)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Completed") },
                        onClick = { onStatusChange(AdminAppointmentStatus.Completed); showMenu = false },
                        leadingIcon = {
                            if (booking.status == AdminAppointmentStatus.Completed) {
                                Icon(Icons.Outlined.AccessTime, null, tint = Color(0xFF0277BD))
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cancelled", color = Color.Red) },
                        onClick = { onStatusChange(AdminAppointmentStatus.Cancelled); showMenu = false },
                        leadingIcon = {
                            if (booking.status == AdminAppointmentStatus.Cancelled) {
                                Icon(Icons.Outlined.AccessTime, null, tint = Color(0xFFC62828))
                            }
                        }
                    )

                    HorizontalDivider(color = MiraBorder.copy(alpha = 0.5f))

                    DropdownMenuItem(
                        text = { Text("Message Client") },
                        onClick = {
                            val sessionId = ChatUtils.getDeterministicChatId(booking.specialistId, booking.customerId)
                            onNavigate("Chat/$sessionId")
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Chat, null, tint = MiraCoral) }
                    )
                }
            }
        }
    }
}

@Composable
fun InfoItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = Color.Gray)
    }
}

@Composable
fun StatusBadge(status: AdminAppointmentStatus) {
    val (text, containerColor, textColor) = when (status) {
        AdminAppointmentStatus.Confirmed -> Triple("Booked", Color(0xFFFFF3E0), Color(0xFFE65100)) // Warm orange/neutral instead of green
        AdminAppointmentStatus.Completed -> Triple("Completed", Color(0xFFE1F5FE), Color(0xFF0277BD))
        AdminAppointmentStatus.Cancelled -> Triple("Cancelled", Color(0xFFFFEBEE), Color(0xFFC62828))
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Circuit [Ui.Factory] binding [DesktopScreen.Bookings] to [BookingsScreenUi]. */
class BookingsUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Bookings -> ui<BookingsUiState> { state, modifier ->
            BookingsScreenUi(
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
