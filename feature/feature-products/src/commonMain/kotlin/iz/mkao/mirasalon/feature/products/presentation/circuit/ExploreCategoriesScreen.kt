package iz.mkao.mirasalon.feature.products.presentation.circuit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.components.MiraEmptyState
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.ProductVariation
import iz.mkao.mirasalon.feature.products.presentation.circuit.components.ExploreProductItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreCategoriesContent(
    state: ExploreCategoriesState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MiraTopAppBar(
                title = "Explore Categories",
                onBackClick = { state.eventSink(ExploreCategoriesEvent.Back) },
                actions = {
                    IconButton(onClick = { state.eventSink(ExploreCategoriesEvent.CartClicked) }) {
                        BadgedBox(
                            badge = {
                                if (state.cartItemCount > 0) {
                                    Badge {
                                        Text(text = state.cartItemCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = "Cart",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
        ) {
            FilterBar(
                selectedCategory = state.selectedCategory,
                selectedVariation = state.selectedVariation,
                productCountLabel = "${state.products.size} Product",
                categories = state.categories,
                onCategoryClick = { /* Handled via dropdown in FilterBar */ },
                onVariationClick = { /* Handled via dropdown in FilterBar */ },
                onCategorySelected = { state.eventSink(ExploreCategoriesEvent.CategorySelected(it)) },
                onVariationSelected = { state.eventSink(ExploreCategoriesEvent.VariationSelected(it)) },
                modifier = Modifier.padding(vertical = 16.dp),
            )

            if (state.isLoading && !state.isRefreshing) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ShimmerLoading()
                }
            } else if (state.products.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MiraEmptyState(
                        message = "No products found",
                        description = "We couldn't find any products in this category. Try adjusting your filters or search query.",
                        icon = Icons.Outlined.Inventory2
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(items = state.products, key = { it.id }) { product ->
                        val isSale = state.promotions.any { promo ->
                            val isTargeted = !promo.applicableServices.isNullOrEmpty() || !promo.applicableCategories.isNullOrEmpty()
                            if (!isTargeted) return@any true

                            val matchesService = product.id in (promo.applicableServices ?: emptyList())
                            val matchesCategory = product.category in (promo.applicableCategories ?: emptyList())
                            matchesService || matchesCategory
                        }
                        ExploreProductItem(
                            product = product,
                            isSale = isSale,
                            onClick = { state.eventSink(ExploreCategoriesEvent.ProductClicked(product.id)) },
                            onAddClick = { state.eventSink(ExploreCategoriesEvent.AddToCart(product.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterBar(
    selectedCategory: ProductCategory?,
    selectedVariation: ProductVariation?,
    productCountLabel: String,
    categories: List<ProductCategory>,
    onCategoryClick: () -> Unit,
    onVariationClick: () -> Unit,
    onCategorySelected: (ProductCategory?) -> Unit,
    onVariationSelected: (ProductVariation?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var variationExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Category Filter
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.clickable { categoryExpanded = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedCategory?.name ?: "Category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All") },
                    onClick = {
                        onCategorySelected(null)
                        categoryExpanded = false
                    }
                )
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            onCategorySelected(category)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // Variation Filter
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.clickable { variationExpanded = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedVariation?.displayName ?: "Variations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = variationExpanded,
                onDismissRequest = { variationExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All") },
                    onClick = {
                        onVariationSelected(null)
                        variationExpanded = false
                    }
                )
                ProductVariation.entries.forEach { variation ->
                    DropdownMenuItem(
                        text = { Text(variation.displayName) },
                        onClick = {
                            onVariationSelected(variation)
                            variationExpanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = productCountLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
