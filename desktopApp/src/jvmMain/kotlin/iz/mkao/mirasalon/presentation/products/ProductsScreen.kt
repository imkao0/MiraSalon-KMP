package iz.mkao.mirasalon.presentation.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import iz.mkao.mirasalon.core.designsystem.theme.*
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.components.CategoryDialog
import iz.mkao.mirasalon.presentation.components.DesktopLoadingState
import iz.mkao.mirasalon.presentation.components.DesktopShell

@Composable
fun ProductsScreenUi(
    state: ProductsUiState,
    modifier: Modifier = Modifier
) {
    val products = state.products
    val isLoading = state.isLoadingProducts
    val snackbarHostState = remember { SnackbarHostState() }

    var showProductDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        DesktopShell(
            title = "Products Management",
            subtitle = "Track and manage salon products",
            selectedRoute = "Products"
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { state.eventSink(ProductsEvent.CategorySelected(null)) },
                        label = { Text("All Products") },
                        shape = RoundedCornerShape(RadiusSmall),
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MiraFaintGray,
                            labelColor = MiraTextSecondary,
                            selectedContainerColor = MiraCoral,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                items(state.categories) { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { state.eventSink(ProductsEvent.CategorySelected(category)) },
                        label = { Text(category) },
                        shape = RoundedCornerShape(RadiusSmall),
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MiraFaintGray,
                            labelColor = MiraTextSecondary,
                            selectedContainerColor = MiraCoral,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                item {
                    IconButton(
                        onClick = { showCategoryDialog = true },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MiraCoral.copy(alpha = 0.1f),
                            contentColor = MiraCoral
                        )
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Products List (${products.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(32.dp))

                    Surface(
                        modifier = Modifier.width(300.dp).height(40.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, MiraFaintGray)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp), tint = MiraTextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { state.eventSink(ProductsEvent.Search(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 14.sp, color = MiraTextPrimary),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (state.searchQuery.isEmpty()) {
                                        Text("Search products...", fontSize = 14.sp, color = MiraTextSecondary)
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        selectedProduct = null
                        showProductDialog = true
                    },
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MiraCoral)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Product")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading && products.isEmpty()) {
                DesktopLoadingState()
            } else if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No products found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f).background(MiraBorder.copy(alpha = 0.5f)),
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(products.distinctBy { it.id }, key = { it.id }) { product ->
                            AdminProductCard(
                                product = product,
                                onEdit = {
                                    selectedProduct = product
                                    showProductDialog = true
                                },
                                onDelete = {
                                    state.eventSink(ProductsEvent.DeleteProduct(product.id))
                                }
                            )
                        }
                    }

                    if (state.totalPages > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { state.eventSink(ProductsEvent.PageChanged(state.currentPage - 1)) },
                                enabled = state.currentPage > 1
                            ) {
                                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous")
                            }

                            Text(
                                "Page ${state.currentPage} of ${state.totalPages}",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            IconButton(
                                onClick = { state.eventSink(ProductsEvent.PageChanged(state.currentPage + 1)) },
                                enabled = state.currentPage < state.totalPages
                            ) {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next")
                            }
                        }
                    }
                }
            }
        }

        if (showProductDialog) {
            ProductDialog(
                product = selectedProduct,
                state = state,
                onDismiss = { showProductDialog = false }
            )
        }

        if (showCategoryDialog) {
            CategoryDialog(
                title = "Add Product Category",
                extraLabel = "Description (optional)",
                onDismiss = { showCategoryDialog = false },
                onConfirm = { name, desc ->
                    state.eventSink(ProductsEvent.CreateCategory(name, null, desc))
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 24.dp)
                .width(350.dp)
        )
    }
}

@Composable
fun AdminProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.White)
            .clickable { onEdit() }
    ) {
        // 1. Background Image
        val imageUrl = product.imageUrl
        if (imageUrl.isNotBlank()) {
            val fullUrl = ApiEndpoints.resolveImageUrl(imageUrl)
            AsyncImage(
                model = fullUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    Napier.e(it.result.throwable) { "Coil failed to load product image: $fullUrl" }
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Image,
                    null,
                    modifier = Modifier.size(80.dp),
                    tint = MiraTextSecondary.copy(alpha = 0.1f)
                )
            }
        }

        // 2. White Fade Gradient at the bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.5f),
                            Color.White
                        ),
                        startY = 100f
                    )
                )
        )

        // 3. Content on top
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = "ID: ${product.id.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiraTextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        letterSpacing = 1.sp
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            null,
                            tint = MiraTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            null,
                            tint = MiraCoral,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))


            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MiraTextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (product.stockQuantity > 0) "${product.stockQuantity} IN STOCK" else "OUT OF STOCK",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stockQuantity > 0) MiraGreen else MiraCoral,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$${product.discountedPrice}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

/** Circuit [Ui.Factory] binding [DesktopScreen.Products] to [ProductsScreenUi]. */
class ProductsUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Products -> ui<ProductsUiState> { state, _ ->
            ProductsScreenUi(
                state = state
            )
        }
        else -> null
    }
}
