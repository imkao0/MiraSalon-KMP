package iz.mkao.mirasalon.presentation.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.designsystem.theme.MiraSuccess
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.domain.model.AdminSpecialist
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.LocalDesktopNavigate
import iz.mkao.mirasalon.presentation.LocalProfileClick
import iz.mkao.mirasalon.presentation.LocalSidebarExpanded
import iz.mkao.mirasalon.presentation.LocalToggleSidebar
import iz.mkao.mirasalon.presentation.components.DesktopLoadingState
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardHeader
import iz.mkao.mirasalon.presentation.dashboard.components.Sidebar

val VelvetaCoral = Color(0xFFF06A6A)

@Composable
fun StaffScreenUi(
    state: StaffUiState,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    isSidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    onProfileClick: () -> Unit
) {
    val staffMembers = state.staff
    val isLoading = state.isLoading
    val stats = state.selectedStaffStats
    val isStatsLoading = state.isStatsLoading
    val uploadProgress = state.uploadProgress

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedStaffForDetail by remember { mutableStateOf<AdminSpecialist?>(null) }

    if (showAddDialog) {
        StaffDialog(
            allServices = state.allServices,
            onUploadImage = { bytes, name, onResult ->
                state.eventSink(StaffEvent.UploadImage(bytes, name, onResult))
            },
            onDismiss = { showAddDialog = false },
            onConfirm = { name, role, years, services, imageUrl ->
                state.eventSink(
                    StaffEvent.CreateStaff(
                        AdminSpecialist(
                            id = "",
                            salonId = "main-salon",
                            name = name,
                            role = role,
                            yearsOfExperience = years,
                            services = services,
                            imageUrl = imageUrl
                        )
                    )
                )
                showAddDialog = false
            }
        )
    }

    selectedStaffForDetail?.let { staff ->
        StaffDetailDialog(
            staff = staff,
            stats = stats,
            isLoadingStats = isStatsLoading,
            uploadProgress = uploadProgress,
            onDismiss = { selectedStaffForDetail = null },
            onUpdateShifts = { shifts ->
                state.eventSink(StaffEvent.UpdateShifts(staff.id, shifts))
            },
            onUploadImage = { bytes, name, onResult ->
                state.eventSink(StaffEvent.UploadImage(bytes, name, onResult))
            },
            onUpdateInfo = { updatedStaff ->
                state.eventSink(StaffEvent.UpdateStaff(updatedStaff))
            },
            allServices = state.allServices,
            readOnly = false
        )
    }

    Row(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Sidebar(
            isExpanded = isSidebarExpanded,
            onToggle = onToggleSidebar,
            selectedRoute = "Staff",
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxHeight().width(if (isSidebarExpanded) 280.dp else 80.dp)
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 40.dp, vertical = 24.dp)) {
            DashboardHeader(
                title = "Staff",
                onProfileClick = onProfileClick
            )

            Spacer(modifier = Modifier.height(32.dp))

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.width(400.dp).height(40.dp),
                    color = Color(0xFFF9F9F9),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { state.eventSink(StaffEvent.Search(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 14.sp, color = MiraTextPrimary),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (state.searchQuery.isEmpty()) {
                                    Text("Search", fontSize = 14.sp, color = Color.Gray)
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeaderAction(icon = Icons.Outlined.FilterList, label = "Filters")
                    Spacer(modifier = Modifier.width(12.dp))
                    HeaderAction(icon = Icons.Outlined.FileDownload, label = "Export")
                    Spacer(modifier = Modifier.width(20.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VelvetaCoral),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("Add Staff", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading && staffMembers.isEmpty()) {
                DesktopLoadingState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(staffMembers.distinctBy { it.id }, key = { it.id }) { staff ->
                        StaffCard(
                            staff = staff,
                            onStatusChange = { isAvailable ->
                                state.eventSink(StaffEvent.SetAvailability(staff.id, isAvailable))
                            },
                            onClick = {
                                selectedStaffForDetail = staff
                                state.eventSink(StaffEvent.LoadStats(staff.id))
                            },
                            onMessageClick = {
                                val sessionId = ChatUtils.getDeterministicChatId("admin", staff.id)
                                onNavigate("Chat/$sessionId")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderAction(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        color = Color.White,
        modifier = Modifier.clickable { }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MiraTextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = MiraTextPrimary)
        }
    }
}

@Composable
fun StaffCard(
    staff: AdminSpecialist,
    onStatusChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(80.dp)
                ) {
                    // Image container
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                    ) {
                        if (!staff.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = staff.imageUrl,
                                contentDescription = staff.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onError = {
                                    Napier.e(it.result.throwable) { "Coil failed to load staff card image: ${staff.imageUrl}" }
                                }
                            )
                        } else {
                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                        }
                    }

                    // Status indicator - offset to cut half image and be partially out of the card area
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 10.dp, y = 10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(if (staff.isAvailable) MiraSuccess else Color.Gray)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = staff.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiraTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isSenior = staff.role.contains("Senior", ignoreCase = true)
                            Surface(
                                color = if (isSenior) Color(0xFFEDE7F6) else Color(0xFFE1F5FE),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCut,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (isSenior) Color(0xFF5E35B1) else Color(0xFF0288D1)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = staff.role.ifBlank { "Stylist" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSenior) Color(0xFF5E35B1) else Color(0xFF0288D1)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Set Online") },
                                        onClick = {
                                            onStatusChange(true)
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(MiraSuccess)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Set Offline") },
                                        onClick = {
                                            onStatusChange(false)
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Gray)
                                            )
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Message") },
                                        onClick = {
                                            onMessageClick()
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.Chat,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = Color.Gray
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = staff.bio.ifBlank { "A highly experienced stylist known for precision cuts and creative coloring. Passionate about transforming hair with expert care." },
                        fontSize = 14.sp,
                        color = Color.Gray,
                        lineHeight = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayServices = staff.services.take(4).map { it.name }.ifEmpty {
                    listOf("Haircuts", "Coloring", "Styling", "Treatments")
                }
                displayServices.forEach { serviceName ->
                    Surface(
                        modifier = Modifier.weight(1f, fill = false),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        color = Color.White
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = serviceName,
                                fontSize = 12.sp,
                                color = MiraTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Circuit [Ui.Factory] binding [DesktopScreen.Staff] to [StaffScreenUi]. */
class StaffUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Staff -> ui<StaffUiState> { state, modifier ->
            StaffScreenUi(
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
