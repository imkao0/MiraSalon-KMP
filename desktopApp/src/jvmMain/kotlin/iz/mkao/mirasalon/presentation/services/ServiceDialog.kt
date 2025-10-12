package iz.mkao.mirasalon.presentation.services

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import kotlinx.coroutines.launch
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ServiceDialog(
    service: Service? = null,
    categories: List<ServiceCategory>,
    state: ServicesUiState,
    onDismiss: () -> Unit
) {
    val isEdit = service != null
    var name by remember { mutableStateOf(service?.name ?: "") }
    var description by remember { mutableStateOf(service?.description ?: "") }
    var priceText by remember { mutableStateOf(service?.price?.toString() ?: "") }
    var durationText by remember { mutableStateOf(service?.durationMinutes?.toString() ?: "30") }
    var selectedCategoryId by remember { mutableStateOf(service?.categoryId ?: categories.firstOrNull()?.id ?: "") }
    var subCategory by remember { mutableStateOf(service?.subCategory ?: "") }
    var imageUrl by remember { mutableStateOf(service?.imageUrl ?: "") }

    var showCategoryMenu by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    val uploadProgress = state.uploadProgress
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Service" else "Add New Service") },
        text = {
            Column(modifier = Modifier.width(450.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, MiraBorder, RoundedCornerShape(4.dp))
                            .clickable {
                                val chooser = JFileChooser()
                                chooser.fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp")
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    scope.launch {
                                        isUploading = true
                                        state.eventSink(ServicesEvent.ResetUploadProgress)
                                        kotlinx.coroutines.suspendCancellableCoroutine<String?> { cont ->
                                            state.eventSink(
                                                ServicesEvent.UploadImage(chooser.selectedFile.readBytes(), chooser.selectedFile.name) { url ->
                                                    cont.resume(url) { _, _, _ -> }
                                                }
                                            )
                                        }.let { url ->
                                            if (url != null) {
                                                imageUrl = url
                                                kotlinx.coroutines.delay(500.milliseconds)
                                            }
                                            isUploading = false
                                        }
                                    }
                                }
                            },
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (imageUrl.isNotEmpty() && !isUploading) {
                                val fullUrl = ApiEndpoints.resolveImageUrl(imageUrl)
                                AsyncImage(
                                    model = fullUrl,
                                    contentDescription = "Service Image",
                                    modifier = Modifier.fillMaxSize(),
                                    onError = {
                                        Napier.e(it.result.throwable) { "Coil failed to load service dialog image: $fullUrl" }
                                    }
                                )
                            } else if (!isUploading) {
                                Icon(Icons.Outlined.AddPhotoAlternate, null, tint = MiraTextSecondary)
                            }

                            if (isUploading) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ShimmerLoading(
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column {
                        Text("Service Image", fontSize = 12.sp, color = MiraTextSecondary)
                        if (imageUrl.isNotEmpty()) {
                            TextButton(onClick = { imageUrl = "" }) {
                                Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp), tint = MiraCoral)
                                Spacer(Modifier.width(4.dp))
                                Text("Remove", color = MiraCoral, fontSize = 12.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(2.dp),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showCategoryMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(categories.find { it.id == selectedCategoryId }?.name ?: "Select Category")
                        }
                        DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = subCategory,
                        onValueChange = { subCategory = it },
                        label = { Text("Sub-Category") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        placeholder = { Text("e.g. Women, Men, Kids") },
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price ($)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Duration (min)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(2.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    val d = durationText.toIntOrNull() ?: 30
                    if (name.isNotBlank() && selectedCategoryId.isNotBlank()) {
                        val sub = subCategory.ifBlank { null }
                        if (isEdit) {
                            state.eventSink(ServicesEvent.UpdateService(service.id, name, selectedCategoryId, sub, p, d, description, imageUrl.ifBlank { null }))
                        } else {
                            state.eventSink(ServicesEvent.CreateService(name, selectedCategoryId, sub, p, d, description, imageUrl.ifBlank { null }))
                        }
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MiraCoral)
            ) {
                Text("Save Service")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
