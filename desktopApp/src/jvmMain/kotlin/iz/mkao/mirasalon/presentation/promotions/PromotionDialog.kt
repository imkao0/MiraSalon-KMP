package iz.mkao.mirasalon.presentation.promotions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.domain.model.AdminDiscountType
import iz.mkao.mirasalon.core.domain.model.AdminPromoStatus
import iz.mkao.mirasalon.core.domain.model.AdminPromotion
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
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

    var selectedFile by remember { mutableStateOf<File?>(null) }

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
                Text("Applicable Services", style = MaterialTheme.typography.labelMedium)
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
                Text("Applicable Categories", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allCats = (productCategories.map { it.name } + serviceCategories.map { it.name }).distinct()
                    allCats.forEach { category ->
                        val isSelected = category in selectedCategories
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategories = if (isSelected) {
                                    selectedCategories.filter { it != category }
                                } else {
                                    selectedCategories + category
                                }
                            },
                            label = { Text(category) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            val chooser = JFileChooser().apply {
                                fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp")
                                dialogTitle = "Select Promotion Image"
                            }
                            val result = chooser.showOpenDialog(null)
                            if (result == JFileChooser.APPROVE_OPTION) {
                                selectedFile = chooser.selectedFile
                            }
                        },
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(if (selectedFile == null && (promotion?.imageUrl?.isBlank() != false)) "Select Image" else "Change Image")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        selectedFile?.name ?: if (promotion?.imageUrl?.isNotBlank() == true) "Existing Image" else "No image selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedFile == null && (promotion?.imageUrl?.isBlank() != false)) Color.Gray else MaterialTheme.colorScheme.primary
                    )
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
                                imageUrl = promotion?.imageUrl
                            )
                        )
                    }
                },
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
