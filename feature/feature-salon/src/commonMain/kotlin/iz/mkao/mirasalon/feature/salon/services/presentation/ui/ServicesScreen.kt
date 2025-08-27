package iz.mkao.mirasalon.feature.salon.services.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.RadiusMedium
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.util.toPriceString
import iz.mkao.mirasalon.feature.salon.Res
import iz.mkao.mirasalon.feature.salon.no_services_found
import iz.mkao.mirasalon.feature.salon.retry
import iz.mkao.mirasalon.feature.salon.search_services_placeholder
import iz.mkao.mirasalon.feature.salon.services.presentation.circuit.ServicesEvent
import iz.mkao.mirasalon.feature.salon.services.presentation.circuit.ServicesState
import iz.mkao.mirasalon.feature.salon.services.presentation.circuit.SortOrder
import iz.mkao.mirasalon.feature.salon.services.presentation.components.CategorySelectorRow
import iz.mkao.mirasalon.feature.salon.services_title
import iz.mkao.mirasalon.feature.salon.try_different_search
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreenContent(
    state: ServicesState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MiraTopAppBar(
                title = stringResource(Res.string.services_title),
                onBackClick = { state.eventSink(ServicesEvent.BackClicked) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    if (state.services.isNotEmpty()) {
                        IconButton(onClick = { state.eventSink(ServicesEvent.ToggleSortOrder) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = if (state.sortOrder == SortOrder.ASCENDING) "Sort Z-A" else "Sort A-Z",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (!state.isCategoryFixed) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { state.eventSink(ServicesEvent.SearchQueryChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(Res.string.search_services_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            // Search is usually reactive, but closing keyboard is good
                        }
                    )
                )

                if (state.categories.isNotEmpty()) {
                    CategorySelectorRow(
                        categories = state.categories,
                        selectedCategoryId = state.selectedCategoryId,
                        onSelect = { state.eventSink(ServicesEvent.CategorySelected(it)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                state.categories.find { it.id == state.selectedCategoryId }?.let { category ->
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ShimmerLoading()
                }
            } else if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { state.eventSink(ServicesEvent.Retry) }) {
                            Text(stringResource(Res.string.retry))
                        }
                    }
                }
            } else if (state.services.isEmpty()) {
                val isSearchActive = state.searchQuery.isNotBlank() || state.selectedCategoryId != null
                if (isSearchActive) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(Res.string.no_services_found),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(Res.string.try_different_search),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.services, key = { it.id }) { service ->
                        val category = state.categories.find { it.id == service.categoryId }
                        val isSale = state.promotions.any { promo ->
                            val isTargeted = !promo.applicableServices.isNullOrEmpty() || !promo.applicableCategories.isNullOrEmpty()
                            if (!isTargeted) return@any true

                            val matchesService = service.id in (promo.applicableServices ?: emptyList())
                            val matchesCategory = category?.name in (promo.applicableCategories ?: emptyList())
                            matchesService || matchesCategory
                        }
                        ServiceCard(
                            service = service,
                            categoryName = category?.name,
                            isSale = isSale,
                            onClick = { state.eventSink(ServicesEvent.ServiceClicked(service.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    service: Service,
    categoryName: String?,
    isSale: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(RadiusMedium))
            .background(Color(0xFFF8F9FA))
            .clickable(onClick = onClick),
    ) {
        val resolvedImageUrl = ApiEndpoints.resolveImageUrl(service.imageUrl)

        AsyncImage(
            model = resolvedImageUrl,
            contentDescription = service.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = ColorPainter(Color(0xFFF1F3F4)),
            error = ColorPainter(Color(0xFFFEE2E2))
        )

        if (resolvedImageUrl == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Spa,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            }
        }

        if (isSale || service.discountPercent > 0) {
            Surface(
                color = Color(0xFFEF4444),
                shape = RoundedCornerShape(RadiusMedium),
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "SALE",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.4f to MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            1.0f to MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = categoryName ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (service.discountPercent > 0) {
                            Text(
                                text = service.price.toPriceString(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.LineThrough
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = service.discountedPrice.toPriceString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${service.durationMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}


