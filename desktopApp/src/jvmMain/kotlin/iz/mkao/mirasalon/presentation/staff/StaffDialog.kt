package iz.mkao.mirasalon.presentation.staff

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.domain.model.Service

@Composable
fun StaffDialog(
    allServices: List<Service>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, role: String, yearsOfExperience: Int, services: List<Service>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var yearsOfExperience by remember { mutableStateOf("0") }
    val selectedServices = remember { mutableStateListOf<Service>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Team Member") },
        text = {
            Column(modifier = Modifier.width(500.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(2.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text("Role (e.g. Senior Stylist)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = yearsOfExperience,
                        onValueChange = { if (it.all { char -> char.isDigit() }) yearsOfExperience = it },
                        label = { Text("Years Exp.") },
                        modifier = Modifier.width(100.dp),
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Assigned Services", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allServices.size) { index ->
                            val service = allServices[index]
                            val isSelected = selectedServices.any { it.id == service.id }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedServices.removeAll { it.id == service.id }
                                        else selectedServices.add(service)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!selectedServices.any { it.id == service.id }) selectedServices.add(service)
                                        } else {
                                            selectedServices.removeAll { it.id == service.id }
                                        }
                                    }
                                )
                                Text(service.name, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && role.isNotBlank()) {
                        onConfirm(
                            name,
                            role,
                            yearsOfExperience.toIntOrNull() ?: 0,
                            selectedServices.toList()
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MiraCoral)
            ) {
                Text("Add Specialist")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
