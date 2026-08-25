package iz.mkao.mirasalon.presentation.staff

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.util.formatRating
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraFaintGray
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.domain.model.AdminSpecialist
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistBreak
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistShift
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistStats
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import kotlinx.coroutines.launch
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun StaffDetailDialog(
    staff: AdminSpecialist,
    stats: AdminSpecialistStats?,
    allServices: List<Service> = emptyList(),
    isLoadingStats: Boolean,
    uploadProgress: Float,
    onDismiss: () -> Unit,
    onUpdateShifts: (List<AdminSpecialistShift>) -> Unit,
    onUploadImage: suspend (ByteArray, String, (String?) -> Unit) -> Unit,
    onUpdateInfo: (AdminSpecialist) -> Unit = { },
    readOnly: Boolean = false
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Schedule")

    var editedName by remember { mutableStateOf(staff.name) }
    var editedRole by remember { mutableStateOf(staff.role) }
    var editedBio by remember { mutableStateOf(staff.bio) }
    var editedYears by remember { mutableStateOf(staff.yearsOfExperience.toString()) }
    var editedImageUrl by remember { mutableStateOf(staff.imageUrl) }
    var isUploading by remember { mutableStateOf(false) }
    val editedServices = remember { mutableStateListOf<Service>().apply { addAll(staff.services) } }

    var isEditing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(4.dp),
            color = Color.White
        ) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, MiraBorder).padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(4.dp)).background(Color.White).border(1.dp, MiraBorder, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val imageUrl = editedImageUrl
                                if (imageUrl != null) {
                                    val fullUrl = ApiEndpoints.resolveImageUrl(imageUrl)
                                    AsyncImage(
                                        model = fullUrl,
                                        contentDescription = "Staff Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        onError = {
                                            Napier.e(it.result.throwable) { "Coil failed to load staff detail image: $fullUrl" }
                                        }
                                    )
                                } else {
                                    Icon(Icons.Outlined.Person, null, modifier = Modifier.size(40.dp), tint = MiraTextSecondary)
                                }

                                if (!readOnly) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                            .clickable {
                                                val chooser = JFileChooser()
                                                chooser.fileFilter = FileNameExtensionFilter(
                                                    "Images (jpg, png, webp)", "jpg", "jpeg", "png", "webp"
                                                )
                                                val result = chooser.showOpenDialog(null)
                                                if (result == JFileChooser.APPROVE_OPTION) {
                                                    val file = chooser.selectedFile
                                                    scope.launch {
                                                        isUploading = true
                                                        onUploadImage(file.readBytes(), file.name) { newUrl ->
                                                            isUploading = false
                                                            if (newUrl != null) {
                                                                editedImageUrl = newUrl
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uploadProgress > 0f && uploadProgress < 1f) {
                                            ShimmerLoading(
                                                modifier = Modifier.size(40.dp)
                                            )
                                        } else {
                                            Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }

                        Spacer(modifier = Modifier.width(20.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (isEditing && !readOnly) {
                                OutlinedTextField(
                                    value = editedName,
                                    onValueChange = { editedName = it },
                                    label = { Text("Name") },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    singleLine = true
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = editedRole,
                                        onValueChange = { editedRole = it },
                                        label = { Text("Role") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = editedYears,
                                        onValueChange = { if (it.all { char -> char.isDigit() }) editedYears = it },
                                        label = { Text("Years Exp.") },
                                        modifier = Modifier.width(100.dp),
                                        singleLine = true
                                    )
                                }
                            } else {
                                Text(staff.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(staff.role, color = MiraTextSecondary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("•", color = MiraTextSecondary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("${staff.yearsOfExperience} years exp.", color = MiraTextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!readOnly) {
                                if (isEditing) {
                                    Button(
                                        onClick = {
                                            onUpdateInfo(
                                                staff.copy(
                                                    name = editedName,
                                                    role = editedRole,
                                                    bio = editedBio,
                                                    imageUrl = editedImageUrl,
                                                    yearsOfExperience = editedYears.toIntOrNull() ?: 0,
                                                    services = editedServices.toList()
                                                )
                                            )
                                            isEditing = false
                                        },
                                        enabled = !isUploading,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        shape = RoundedCornerShape(2.dp)
                                    ) {
                                        Text("Save Changes")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = { isEditing = false }) {
                                        Text("Cancel", color = MiraTextSecondary)
                                    }
                                } else {
                                    Button(
                                        onClick = { isEditing = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MiraCoral),
                                        shape = RoundedCornerShape(2.dp)
                                    ) {
                                        Text("Edit Info")
                                    }
                                }
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.padding(start = 8.dp)) {
                            Icon(Icons.Outlined.Close, null)
                        }
                    }
                }

                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = MiraCoral,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(selectedTabIndex = selectedTab),
                            color = MiraCoral
                        )
                    },
                    divider = { HorizontalDivider(color = MiraBorder) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).padding(24.dp)) {
                    when (selectedTab) {
                        0 -> OverviewTab(
                            staff = staff,
                            bio = editedBio,
                            onBioChange = { editedBio = it },
                            stats = stats,
                            allServices = allServices,
                            editedServices = editedServices,
                            isEditing = isEditing,
                            isLoading = isLoadingStats,
                            readOnly = readOnly
                        )
                        1 -> ScheduleTab(staff, onUpdateShifts, onDismiss, readOnly)
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewTab(
    staff: AdminSpecialist,
    bio: String,
    onBioChange: (String) -> Unit,
    stats: AdminSpecialistStats?,
    allServices: List<Service>,
    editedServices: MutableList<Service>,
    isEditing: Boolean,
    isLoading: Boolean,
    readOnly: Boolean = false
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!readOnly) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Revenue",
                    value = "$${stats?.totalRevenue ?: 0.0}",
                    icon = Icons.Outlined.Payments,
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
            }
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Appointments",
                value = "${stats?.serviceCount ?: 0}",
                icon = Icons.Outlined.CalendarMonth,
                color = MaterialTheme.colorScheme.tertiaryContainer,
            )
            if (!readOnly) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Avg. Rating",
                    value = (stats?.averageRating ?: 0.0).formatRating(),
                    icon = Icons.Outlined.Star,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Biography", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (isEditing && !readOnly) {
            OutlinedTextField(
                value = bio,
                onValueChange = onBioChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter specialist biography...") },
                shape = RoundedCornerShape(4.dp),
                minLines = 4
            )
        } else {
            Text(
                text = bio.ifBlank { "No biography provided for this specialist." },
                style = MaterialTheme.typography.bodyMedium,
                color = if (bio.isBlank()) MiraTextSecondary else Color.Black
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isEditing && !readOnly) {
            Text("Assign Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allServices.size) { index ->
                        val service = allServices[index]
                        val isSelected = editedServices.any { it.id == service.id }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) editedServices.removeAll { it.id == service.id }
                                    else editedServices.add(service)
                                }
                                .border(1.dp, MiraBorder, RoundedCornerShape(4.dp))
                                .padding(12.dp)
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!editedServices.any { it.id == service.id }) editedServices.add(service)
                                    } else {
                                        editedServices.removeAll { it.id == service.id }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(service.name)
                        }
                    }
                }
            }
        } else {
            Text("Linked Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (staff.services.isEmpty()) {
                Text("No services linked to this specialist", color = MiraTextSecondary)
            } else {
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    staff.services.forEach { service ->
                        Surface(
                            color = MiraFaintGray,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MiraBorder)
                        ) {
                            Text(
                                service.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Recent Appointments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                ShimmerLoading()
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recent appointments found", color = MiraTextSecondary)
                }
            }
        }
    }
}

@Composable
fun ScheduleTab(
    staff: AdminSpecialist,
    onUpdateShifts: (List<AdminSpecialistShift>) -> Unit,
    onDismiss: () -> Unit,
    readOnly: Boolean = false
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var currentShifts by remember { mutableStateOf(staff.shifts) }
    var editingShift by remember { mutableStateOf<AdminSpecialistShift?>(null) }
    var showShiftDialog by remember { mutableStateOf(false) }
    var selectedDayOfWeek by remember { mutableIntStateOf(1) }

    if (showShiftDialog && !readOnly) {
        ShiftDialog(
            shift = editingShift,
            dayOfWeek = selectedDayOfWeek,
            onDismiss = { showShiftDialog = false },
            onConfirm = { updatedShift ->
                val newShifts = currentShifts.toMutableList()
                val index = newShifts.indexOfFirst { it.dayOfWeek == updatedShift.dayOfWeek }
                if (index != -1) {
                    newShifts[index] = updatedShift
                } else {
                    newShifts.add(updatedShift)
                }
                currentShifts = newShifts
                showShiftDialog = false
            }
        )
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Weekly Working Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(days.size) { index ->
                val dayOfWeek = index + 1
                val shift = currentShifts.find { it.dayOfWeek == dayOfWeek }

                Column(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MiraBorder, RoundedCornerShape(4.dp)).padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(days[index], modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)

                        if (shift != null && shift.isActive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${shift.startTime} - ${shift.endTime}", color = MiraTextPrimary, fontWeight = FontWeight.Medium)
                                if (!readOnly) {
                                    Spacer(modifier = Modifier.width(16.dp))
                                    TextButton(onClick = {
                                        editingShift = shift
                                        selectedDayOfWeek = dayOfWeek
                                        showShiftDialog = true
                                    }) {
                                        Text("Edit", color = MiraCoral)
                                    }
                                }
                            }
                        } else {
                            Text("Day Off", color = MiraTextSecondary)
                            if (!readOnly) {
                                TextButton(onClick = {
                                    editingShift = null
                                    selectedDayOfWeek = dayOfWeek
                                    showShiftDialog = true
                                }) {
                                    Text("Add Shift", color = MiraCoral)
                                }
                            }
                        }
                    }

                    if (shift != null && shift.isActive && shift.breaks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Close, null, modifier = Modifier.size(14.dp), tint = MiraCoral)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Breaks:", style = MaterialTheme.typography.bodySmall, color = MiraTextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            shift.breaks.forEach { b ->
                                Surface(
                                    color = MiraCoral.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        "${b.title}: ${b.startTime}-${b.endTime}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MiraCoral
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Upcoming Absences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                if (staff.absences.isEmpty()) {
                    Text("No scheduled absences", color = MiraTextSecondary, style = MaterialTheme.typography.bodyMedium)
                } else {
                    staff.absences.forEach { absence ->
                         Row(
                            modifier = Modifier.fillMaxWidth().border(1.dp, MiraBorder, RoundedCornerShape(4.dp)).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(absence.reason ?: "Away", fontWeight = FontWeight.Medium)
                                // Simplified date formatting for demonstration
                                Text("Duration: Temporary", style = MaterialTheme.typography.bodySmall, color = MiraTextSecondary)
                            }
                            Icon(Icons.Outlined.Close, null, tint = MiraCoral, modifier = Modifier.clickable { /* Handle delete */ })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!readOnly) {
            Button(
                onClick = { 
                    onUpdateShifts(currentShifts)
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = MiraCoral),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text("Save Schedule")
            }
        }
    }
}

@Composable
fun ShiftDialog(
    shift: AdminSpecialistShift?,
    dayOfWeek: Int,
    onDismiss: () -> Unit,
    onConfirm: (AdminSpecialistShift) -> Unit
) {
    var startTime by remember { mutableStateOf(shift?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(shift?.endTime ?: "17:00") }
    val breaks = remember { mutableStateListOf<AdminSpecialistBreak>().apply { addAll(shift?.breaks ?: emptyList()) } }
    
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(500.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    if (shift == null) "Add Shift for ${days[dayOfWeek - 1]}" else "Edit Shift for ${days[dayOfWeek - 1]}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Working Hours", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("09:00") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("17:00") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Breaks (Lunch, etc.)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { 
                        breaks.add(AdminSpecialistBreak(startTime = "12:00", endTime = "13:00", title = "Lunch"))
                    }) {
                        Text("+ Add Break", color = MiraCoral)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                breaks.forEachIndexed { index, b ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = b.title, onValueChange = { breaks[index] = b.copy(title = it) }, modifier = Modifier.weight(1.5f), label = { Text("Label") })
                        OutlinedTextField(value = b.startTime, onValueChange = { breaks[index] = b.copy(startTime = it) }, modifier = Modifier.weight(1f), label = { Text("Start") })
                        OutlinedTextField(value = b.endTime, onValueChange = { breaks[index] = b.copy(endTime = it) }, modifier = Modifier.weight(1f), label = { Text("End") })
                        IconButton(onClick = { breaks.removeAt(index) }) {
                            Icon(Icons.Outlined.Close, null, tint = MiraCoral)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MiraTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                AdminSpecialistShift(
                                    id = shift?.id ?: "",
                                    specialistId = shift?.specialistId ?: "",
                                    dayOfWeek = dayOfWeek,
                                    startTime = startTime,
                                    endTime = endTime,
                                    isActive = true,
                                    breaks = breaks.toList()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MiraCoral),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}


@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        border = BorderStroke(1.dp, MiraBorder),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MiraTextPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = MiraTextSecondary)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
