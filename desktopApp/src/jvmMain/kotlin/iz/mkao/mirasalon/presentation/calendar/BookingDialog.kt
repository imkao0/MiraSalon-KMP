package iz.mkao.mirasalon.presentation.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import iz.mkao.mirasalon.core.designsystem.theme.*
import iz.mkao.mirasalon.core.domain.model.*
import kotlinx.datetime.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookingDialog(
    initialDate: LocalDate,
    specialists: List<Specialist>,
    services: List<Service>,
    customers: List<CustomerSummary>,
    onSave: (
        customerId: String,
        specialistId: String,
        serviceIds: List<String>,
        date: LocalDate,
        time: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedSpecialist by remember { mutableStateOf<Specialist?>(null) }
    var selectedServices by remember { mutableStateOf(setOf<Service>()) }
    var selectedCustomer by remember { mutableStateOf<CustomerSummary?>(null) }
    var selectedTime by remember { mutableStateOf("09:00") }

    var step by remember { mutableIntStateOf(1) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.width(700.dp).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(2.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "New Booking",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MiraTextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, null, tint = MiraTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    StepItem(1, "Service & Client", step >= 1, modifier = Modifier.weight(1f))
                    StepItem(2, "Staff & Schedule", step >= 2, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (step == 1) {
                        StepOneContent(
                            customers = customers,
                            selectedCustomer = selectedCustomer,
                            onCustomerSelect = { selectedCustomer = it },
                            services = services,
                            selectedServices = selectedServices,
                            onServiceToggle = { service ->
                                if (selectedServices.contains(service)) {
                                    selectedServices = selectedServices - service
                                } else {
                                    selectedServices = selectedServices + service
                                }
                            }
                        )
                    } else {
                        StepTwoContent(
                            specialists = specialists,
                            selectedSpecialist = selectedSpecialist,
                            onSpecialistSelect = { selectedSpecialist = it },
                            selectedDate = selectedDate,
                            onDateChange = { selectedDate = it },
                            selectedTime = selectedTime,
                            onTimeChange = { selectedTime = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { step-- },
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, MiraBorder)
                        ) {
                            Text("Back", color = MiraTextPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Button(
                        onClick = {
                            if (step == 1) {
                                step = 2
                            } else {
                                val customer = selectedCustomer
                                val specialist = selectedSpecialist
                                if (customer != null && specialist != null && selectedServices.isNotEmpty()) {
                                    onSave(
                                        customer.id,
                                        specialist.id,
                                        selectedServices.map { it.id },
                                        selectedDate,
                                        selectedTime
                                    )
                                }
                            }
                        },
                        enabled = if (step == 1) selectedCustomer != null && selectedServices.isNotEmpty() else selectedSpecialist != null,
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MiraCoral)
                    ) {
                        Text(if (step == 1) "Next" else "Confirm Booking")
                    }
                }
            }
        }
    }
}

@Composable
fun StepItem(number: Int, label: String, isActive: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isActive) MiraCoral else MiraBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MiraTextPrimary else MiraTextSecondary
        )
    }
}

@Composable
fun StepOneContent(
    customers: List<CustomerSummary>,
    selectedCustomer: CustomerSummary?,
    onCustomerSelect: (CustomerSummary) -> Unit,
    services: List<Service>,
    selectedServices: Set<Service>,
    onServiceToggle: (Service) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Select Client", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        var clientSearch by remember { mutableStateOf("") }
        OutlinedTextField(
            value = clientSearch,
            onValueChange = { clientSearch = it },
            placeholder = { Text("Search customer name") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            shape = RoundedCornerShape(2.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MiraCoral,
                unfocusedBorderColor = MiraBorder
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f).border(BorderStroke(1.dp, MiraBorder), RoundedCornerShape(2.dp))) {
            items(customers.filter { it.name.contains(clientSearch, true) }) { customer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustomerSelect(customer) }
                        .background(if (selectedCustomer?.id == customer.id) MiraCoral.copy(alpha = 0.1f) else Color.Transparent)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White).border(0.5.dp, MiraBorder, CircleShape), contentAlignment = Alignment.Center) {
                        Text(customer.name.take(1).uppercase())
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(customer.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(customer.email, style = MaterialTheme.typography.bodySmall, color = MiraTextSecondary)
                    }
                }
                HorizontalDivider(color = MiraBorder)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Select Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f).border(BorderStroke(1.dp, MiraBorder), RoundedCornerShape(2.dp))) {
            items(services) { service ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onServiceToggle(service) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedServices.contains(service),
                        onCheckedChange = { onServiceToggle(service) },
                        colors = CheckboxDefaults.colors(checkedColor = MiraCoral)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(service.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("${service.durationMinutes} min", style = MaterialTheme.typography.bodySmall, color = MiraTextSecondary)
                    }
                    Text("$${service.price}", fontWeight = FontWeight.Bold, color = MiraTextPrimary)
                }
                HorizontalDivider(color = MiraBorder)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepTwoContent(
    specialists: List<Specialist>,
    selectedSpecialist: Specialist?,
    onSpecialistSelect: (Specialist) -> Unit,
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    selectedTime: String,
    onTimeChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Select Specialist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            specialists.forEach { specialist ->
                Card(
                    onClick = { onSpecialistSelect(specialist) },
                    modifier = Modifier.width(140.dp).fillMaxHeight(),
                    shape = RoundedCornerShape(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedSpecialist?.id == specialist.id) MiraCoral.copy(alpha = 0.1f) else Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selectedSpecialist?.id == specialist.id) MiraCoral else MiraBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).border(0.5.dp, MiraBorder, CircleShape), contentAlignment = Alignment.Center) {
                            Text(specialist.name.take(1))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(specialist.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(specialist.role, style = MaterialTheme.typography.labelSmall, color = MiraTextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Select Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                val times = listOf("09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "13:00", "13:30", "14:00", "14:30", "15:00")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    times.forEach { time ->
                        val isSelected = selectedTime == time
                        Surface(
                            modifier = Modifier
                                .clickable { onTimeChange(time) }
                                .width(80.dp),
                            shape = RoundedCornerShape(2.dp),
                            color = if (isSelected) MiraCoral else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) MiraCoral else MiraBorder)
                        ) {
                            Text(
                                text = time,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                color = if (isSelected) Color.White else MiraTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
