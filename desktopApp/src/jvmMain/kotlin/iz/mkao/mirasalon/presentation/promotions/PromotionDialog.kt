import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.domain.model.AdminDiscountType
import iz.mkao.mirasalon.core.domain.model.AdminPromoStatus
import iz.mkao.mirasalon.core.domain.model.AdminPromotion
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun PromotionDialog(
    promotion: AdminPromotion? = null,
    services: List<Service> = emptyList(),
    productCategories: List<ProductCategory> = emptyList(),
    serviceCategories: List<ServiceCategory> = emptyList(),
    uploadProgress: Float,
    onUploadImage: suspend (ByteArray, String, (String?) -> Unit) -> Unit,
    onResetUpload: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (AdminPromotion) -> Unit
) {
    var code by remember { mutableStateOf(promotion?.code ?: "") }
    var description by remember { mutableStateOf(promotion?.description ?: "") }
    var discountValue by remember { mutableStateOf(promotion?.discountValue?.toString() ?: "") }
    var discountType by remember { mutableStateOf(promotion?.discountType ?: AdminDiscountType.Percentage) }

    var selectedServices by remember { mutableStateOf(promotion?.applicableServices ?: emptyList()) }
    var selectedCategories by remember { mutableStateOf(promotion?.applicableCategories ?: emptyList()) }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var validFromStr by remember {
        mutableStateOf(promotion?.validFrom?.let { dateFormatter.format(Date(it)) } ?: dateFormatter.format(Date()))
    }
    var validUntilStr by remember {
        mutableStateOf(promotion?.validUntil?.let { dateFormatter.format(Date(it)) } ?: "")
    }

    var totalRedemptions by remember { mutableStateOf(promotion?.totalRedemptions?.toString() ?: "") }
    var perUserRedemptions by remember { mutableStateOf(promotion?.perUserRedemptions?.toString() ?: "1") }
    var minSpend by remember { mutableStateOf(promotion?.minOrderValue?.toString() ?: "0") }
    var status by remember { mutableStateOf(promotion?.status ?: AdminPromoStatus.Active) }

    var imageUrl by remember { mutableStateOf(promotion?.imageUrl ?: "") }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (promotion == null) "Create Promotion" else "Edit Promotion") },
        text = {
            Column(modifier = Modifier.width(500.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Promo Code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(2.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(2.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Type: ${discountType.name}")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            AdminDiscountType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        discountType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = discountValue,
                        onValueChange = { discountValue = it },
                        label = { Text("Value") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    OutlinedTextField(
                        value = validFromStr,
                        onValueChange = { validFromStr = it },
                        label = { Text("From (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = validUntilStr,
                        onValueChange = { validUntilStr = it },
                        label = { Text("Until (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    OutlinedTextField(
                        value = totalRedemptions,
                        onValueChange = { totalRedemptions = it },
                        label = { Text("Total Limit (opt)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = perUserRedemptions,
                        onValueChange = { perUserRedemptions = it },
                        label = { Text("Per User Limit") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    OutlinedTextField(
                        value = minSpend,
                        onValueChange = { minSpend = it },
                        label = { Text("Min Order ($)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Status: ${status.name}")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            AdminPromoStatus.entries.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        status = s
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Applicable Product Categories", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    productCategories.forEach { category ->
                        val isSelected = category.name in selectedCategories
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategories = if (isSelected) {
                                    selectedCategories.filter { it != category.name }
                                } else {
                                    selectedCategories + category.name
                                }
                            },
                            label = { Text(category.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Applicable Service Categories", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    serviceCategories.forEach { category ->
                        val isSelected = category.name in selectedCategories
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategories = if (isSelected) {
                                    selectedCategories.filter { it != category.name }
                                } else {
                                    selectedCategories + category.name
                                }
                            },
                            label = { Text(category.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Individual Services (Optional)", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    services.forEach { service ->
                        val isSelected = service.id in selectedServices
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedServices = if (isSelected) {
                                    selectedServices.filter { it != service.id }
                                } else {
                                    selectedServices + service.id
                                }
                            },
                            label = { Text(service.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, MiraBorder, RoundedCornerShape(4.dp)),
                        color = Color(0xFFF9F9F9)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (imageUrl.isNotEmpty() && !isUploading) {
                                AsyncImage(
                                    model = ApiEndpoints.resolveImageUrl(imageUrl),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (!isUploading) {
                                Icon(Icons.Outlined.Image, null, modifier = Modifier.size(32.dp), tint = Color.Gray.copy(alpha = 0.5f))
                            }

                            if (isUploading) {
                                ShimmerLoading(modifier = Modifier.size(40.dp))
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Promotion Image", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val chooser = JFileChooser().apply {
                                        fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp")
                                        dialogTitle = "Select Promotion Image"
                                    }
                                    val result = chooser.showOpenDialog(null)
                                    if (result == JFileChooser.APPROVE_OPTION) {
                                        val file = chooser.selectedFile
                                        scope.launch {
                                            isUploading = true
                                            onResetUpload()
                                            onUploadImage(file.readBytes(), file.name) { newUrl ->
                                                isUploading = false
                                                if (newUrl != null) imageUrl = newUrl
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                enabled = !isUploading
                            ) {
                                Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Image")
                            }

                            if (imageUrl.isNotEmpty()) {
                                TextButton(onClick = { imageUrl = "" }) {
                                    Text("Remove", color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val discV = discountValue.toDoubleOrNull() ?: 0.0
                    val min = minSpend.toDoubleOrNull() ?: 0.0
                    val totalL = totalRedemptions.toIntOrNull()
                    val perUserL = perUserRedemptions.toIntOrNull() ?: 1

                    val fromDate = try { dateFormatter.parse(validFromStr).time } catch (e: Exception) { System.currentTimeMillis() }
                    val untilDate = try { dateFormatter.parse(validUntilStr).time } catch (e: Exception) { null }

                    if (code.isNotBlank() && discV > 0) {
                        onConfirm(
                            AdminPromotion(
                                id = promotion?.id ?: "",
                                code = code,
                                description = description,
                                discountType = discountType,
                                discountValue = discV,
                                validFrom = fromDate,
                                validUntil = untilDate,
                                totalRedemptions = totalL,
                                perUserRedemptions = perUserL,
                                minOrderValue = min,
                                applicableServices = selectedServices,
                                applicableCategories = selectedCategories,
                                status = status,
                                imageUrl = imageUrl
                            )
                        )
                    }
                },
                enabled = !isUploading && code.isNotBlank() && (discountValue.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MiraCoral)
            ) {
                Text(if (promotion == null) "Create Offer" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
