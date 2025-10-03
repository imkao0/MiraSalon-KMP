package iz.mkao.mirasalon.presentation.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import kotlinx.coroutines.launch
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    product: Product? = null,
    state: ProductsUiState,
    onDismiss: () -> Unit
) {
    val isEdit = product != null
    var name by remember { mutableStateOf(product?.name ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var priceText by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Skincare") }
    var stockText by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "0") }
    var discountText by remember { mutableStateOf(product?.discountPercent?.toString() ?: "0") }
    var imageUrl by remember { mutableStateOf(product?.imageUrl ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    val uploadProgress = state.uploadProgress
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()


    val categories = listOf("Skincare", "Haircare", "Cosmetics", "Fragrance", "Tools", "Accessories")

    val isValid = name.isNotBlank() &&
            description.isNotBlank() &&
            (priceText.toDoubleOrNull() ?: -1.0) >= 0 &&
            (stockText.toIntOrNull() ?: -1) >= 0 &&
            (discountText.toIntOrNull() ?: -1) in 0..100

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.width(520.dp),
            shape = RoundedCornerShape(2.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isEdit) "Edit Product" else "Add new Product",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MiraTextPrimary,
                        fontSize = 20.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = MiraTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .border(1.dp, MiraBorder, RoundedCornerShape(2.dp)),
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (imageUrl.isNotEmpty() && !isUploading) {
                                val fullUrl = ApiEndpoints.resolveImageUrl(imageUrl)
                                AsyncImage(
                                    model = fullUrl,
                                    contentDescription = "Product Image",
                                    modifier = Modifier.fillMaxSize(),
                                    onError = {
                                        Napier.e(it.result.throwable) { "Coil failed to load product dialog image: $fullUrl" }
                                    }
                                )
                            } else if (!isUploading) {
                                Icon(
                                    Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = "Upload image",
                                    tint = MiraBorder,
                                    modifier = Modifier.size(28.dp)
                                )
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Product Image",
                            style = MaterialTheme.typography.labelMedium,
                            color = MiraTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val chooser = JFileChooser()
                                    chooser.fileFilter = FileNameExtensionFilter(
                                        "Images (jpg, png, webp)", "jpg", "jpeg", "png", "webp"
                                    )
                                    val result = chooser.showOpenDialog(null)
                                    if (result == JFileChooser.APPROVE_OPTION) {
                                        val file = chooser.selectedFile
                                        selectedFileName = file.name
                                        scope.launch {
                                            isUploading = true
                                            state.eventSink(ProductsEvent.ResetUploadProgress)
                                            state.eventSink(
                                                ProductsEvent.UploadImage(file.readBytes(), file.name) { url ->
                                                    if (url != null) imageUrl = url
                                                    isUploading = false
                                                }
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(2.dp),
                                enabled = !isUploading
                            ) {
                                if (isUploading) {
                                    ShimmerLoading(
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Icon(Icons.Outlined.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload", fontSize = 13.sp)
                            }
                            if (imageUrl.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        imageUrl = ""
                                        selectedFileName = null
                                    },
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Icon(Icons.Outlined.Delete, null, tint = MiraCoral, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove", color = MiraCoral, fontSize = 13.sp)
                                }
                            }
                        }
                        if (selectedFileName != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                selectedFileName ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MiraTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }


                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name", color = MiraTextSecondary) },
                    placeholder = { Text("Name", color = MiraBorder) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(2.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiraCoral,
                        unfocusedBorderColor = MiraBorder
                    )
                )


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category", color = MiraTextSecondary) },
                            modifier = Modifier.fillMaxWidth().clickable { categoryExpanded = true },
                            trailingIcon = {
                                IconButton(onClick = { categoryExpanded = !categoryExpanded }) {
                                    Icon(
                                        if (categoryExpanded) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            shape = RoundedCornerShape(2.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MiraCoral,
                                unfocusedBorderColor = MiraBorder
                            )
                        )
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },

                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }


                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price", color = MiraTextSecondary) },
                        placeholder = { Text("$ 00.00", color = MiraBorder) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(2.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MiraCoral,
                            unfocusedBorderColor = MiraBorder
                        )
                    )


                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock", color = MiraTextSecondary) },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(2.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MiraCoral,
                            unfocusedBorderColor = MiraBorder
                        )
                    )
                }


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {

                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { newText ->
                            val num = newText.toIntOrNull()
                            if (num == null && newText.isEmpty()) discountText = ""
                            else if (num != null && num in 0..100) discountText = newText
                            else if (newText.isEmpty()) discountText = ""
                        },
                        label = { Text("Discount %", color = MiraTextSecondary) },
                        placeholder = { Text("0", color = MiraBorder) },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(2.dp),
                        suffix = { Text("%") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MiraCoral,
                            unfocusedBorderColor = MiraBorder
                        )
                    )

                    // preview
                    val price = priceText.toDoubleOrNull()
                    val disc = discountText.toIntOrNull() ?: 0
                    if (price != null && disc > 0) {
                        Surface(
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(2.dp),
                            color = MiraCoral.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Sale price", fontSize = 11.sp, color = MiraTextSecondary)
                                    Text(
                                        "$${price * (100 - disc) / 100.0}",
                                        fontWeight = FontWeight.Bold,
                                        color = MiraCoral,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }


                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Product Description", color = MiraTextSecondary) },
                    placeholder = { Text("description", color = MiraBorder) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiraCoral,
                        unfocusedBorderColor = MiraBorder
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, MiraBorder)
                    ) {
                        Text("Cancel", color = MiraTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (isValid) {
                                val currentProduct = product
                                if (isEdit && currentProduct != null) {
                                    state.eventSink(
                                        ProductsEvent.UpdateProduct(
                                            currentProduct.id,
                                            name.trim(),
                                            description.trim(),
                                            priceText.toDouble(),
                                            category,
                                            stockText.toInt(),
                                            imageUrl.takeIf { it.isNotBlank() },
                                            discountText.toIntOrNull() ?: 0
                                        )
                                    )
                                } else {
                                    state.eventSink(
                                        ProductsEvent.CreateProduct(
                                            name.trim(),
                                            description.trim(),
                                            priceText.toDouble(),
                                            category,
                                            stockText.toInt(),
                                            imageUrl.takeIf { it.isNotBlank() },
                                            discountText.toIntOrNull() ?: 0
                                        )
                                    )
                                }
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MiraCoral,
                            disabledContainerColor = MiraCoral.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.defaultMinSize(minWidth = 100.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
