package iz.mkao.mirasalon.feature.salon.services.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.ElevationNone
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.RadiusTiny
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
                    shape = RoundedCornerShape(28.dp),
                    trailingIcon = {
                        Icon(Icons.Outlined.Search, null, tint = MiraTextSecondary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { }
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
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
                        Text(
                            text = "Featured",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${category.name} Services",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Outlined.AutoAwesome, null, tint = MiraCoral, modifier = Modifier.size(24.dp))
                        }
                        Text(
                            text = "Handpicked services just for you",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MiraTextSecondary
                        )
                    }
                }

                if (state.subCategories.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        item {
                            SubCategoryChip(
                                label = "All",
                                isSelected = state.selectedSubCategory == null,
                                onClick = { state.eventSink(ServicesEvent.SubCategorySelected(null)) }
                            )
                        }
                        items(state.subCategories) { subCat ->
                            SubCategoryChip(
                                label = subCat,
                                isSelected = state.selectedSubCategory == subCat,
                                onClick = { state.eventSink(ServicesEvent.SubCategorySelected(subCat)) }
                            )
                        }
                    }
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
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.services.distinctBy { it.id }, key = { it.id }) { service ->
                        val category = state.categories.find { it.id == service.categoryId }
                        ServiceListCard(
                            service = service,
                            categoryName = category?.name,
                            onClick = { state.eventSink(ServicesEvent.ServiceClicked(service.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        selected = isSelected,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ServiceListCard(
    service: Service,
    categoryName: String?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusTiny),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = ElevationNone)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(RadiusTiny))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    Icon(
                        Icons.Outlined.Spa, null,
                        modifier = Modifier.align(Alignment.Center).size(48.dp),
                        tint = MiraTextSecondary.copy(alpha = 0.3f)
                    )
                }

                IconButton(
                    onClick = { /* Toggle favorite */ },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(18.dp), tint = MiraCoral)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MiraTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = service.discountedPrice.toPriceString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MiraCoral.copy(alpha = 0.2f),
                            contentColor = MiraCoral
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Book Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
