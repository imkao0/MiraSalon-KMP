package iz.mkao.mirasalon.presentation.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import kotlinx.coroutines.launch
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun StaffDialog(
    allServices: List<Service>,
    onUploadImage: suspend (ByteArray, String, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, role: String, yearsOfExperience: Int, services: List<Service>, imageUrl: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var yearsOfExperience by remember { mutableStateOf("0") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val selectedServices = remember { mutableStateListOf<Service>() }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Team Member") },
        text = {
            Column(modifier = Modifier.width(500.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                            .border(1.dp, MiraBorder, CircleShape)
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
                                            imageUrl = newUrl
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = ApiEndpoints.resolveImageUrl(imageUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                        }

                        if (isUploading) {
                            ShimmerLoading(modifier = Modifier.matchParentSize())
                        } else {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true
                    )
                }

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
                            selectedServices.toList(),
                            imageUrl
                        )
                    }
                },
                enabled = !isUploading && name.isNotBlank() && role.isNotBlank(),
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
