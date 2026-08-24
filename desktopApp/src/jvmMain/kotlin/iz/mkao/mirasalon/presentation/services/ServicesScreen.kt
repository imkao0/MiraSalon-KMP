package iz.mkao.mirasalon.presentation.services

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraFaintGray
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.LocalDesktopNavigate
import iz.mkao.mirasalon.presentation.LocalProfileClick
import iz.mkao.mirasalon.presentation.LocalSidebarExpanded
import iz.mkao.mirasalon.presentation.LocalToggleSidebar
import iz.mkao.mirasalon.presentation.components.CategoryDialog
import iz.mkao.mirasalon.presentation.components.DesktopLoadingState
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardHeader
import iz.mkao.mirasalon.presentation.dashboard.components.Sidebar

@Composable
fun ServicesScreenUi(
    state: ServicesUiState,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    isSidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    onProfileClick: () -> Unit
) {
    val services = state.services
    val isLoading = state.isLoading
    val snackbarHostState = remember { SnackbarHostState() }

    var showServiceDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedService by remember { mutableStateOf<Service?>(null) }

    Row(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Sidebar(
            isExpanded = isSidebarExpanded,
            onToggle = onToggleSidebar,
            selectedRoute = "Services",
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxHeight().width(if (isSidebarExpanded) 280.dp else 80.dp)
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
            DashboardHeader(
                title = "Service Menu",
                subtitle = "Manage salon services",
                onProfileClick = onProfileClick
            )

            Spacer(modifier = Modifier.height(24.dp))


            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { state.eventSink(ServicesEvent.CategorySelected(null)) },
                        label = { Text("All Services") },
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
                        selected = state.selectedCategoryId == category.id,
                        onClick = { state.eventSink(ServicesEvent.CategorySelected(category.id)) },
                        label = { Text(category.name) },
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
                        "Services List (${services.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MiraTextPrimary
                    )

                    Spacer(modifier = Modifier.width(32.dp))

            
                    Surface(
                        modifier = Modifier.width(300.dp).height(40.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp), tint = MiraTextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { state.eventSink(ServicesEvent.Search(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 14.sp, color = MiraTextPrimary),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (state.searchQuery.isEmpty()) {
                                        Text("Search services...", fontSize = 14.sp, color = MiraTextSecondary)
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        selectedService = null
                        showServiceDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiraCoral),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Service")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading && services.isEmpty()) {
                DesktopLoadingState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.fillMaxSize().background(MiraBorder.copy(alpha = 0.5f))
                ) {
                    items(services) { service ->
                        ServiceCard(
                            service = service,
                            onEdit = {
                                selectedService = service
                                showServiceDialog = true
                            },
                            onDelete = { state.eventSink(ServicesEvent.DeleteService(service.id)) }
                        )
                    }
                }
            }
        }
    }

    if (showServiceDialog) {
        ServiceDialog(
            service = selectedService,
            categories = state.categories,
            state = state,
            onDismiss = { showServiceDialog = false }
        )
    }

    if (showCategoryDialog) {
        CategoryDialog(
            title = "Add Service Category",
            extraLabel = "Icon Name (optional)",
            onDismiss = { showCategoryDialog = false },
            onConfirm = { name, icon ->
                state.eventSink(ServicesEvent.CreateCategory(name, icon, null))
            }
        )
    }
}

@Composable
fun ServiceCard(
    service: Service,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .background(Color.White)
    ) {
        // 1. Background Image
        val imageUrl = service.imageUrl
        if (!imageUrl.isNullOrBlank()) {
            val fullUrl = ApiEndpoints.resolveImageUrl(imageUrl)
            AsyncImage(
                model = fullUrl,
                contentDescription = service.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    Napier.e(it.result.throwable) { "Coil failed to load service image: $fullUrl" }
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Spa,
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
                        startY = 100f // Start fading early
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
                        text = "ID: ${service.id.takeLast(6).uppercase()}",
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
                text = service.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${service.durationMinutes} MIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiraTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$${service.price}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

/** Circuit [Ui.Factory] binding [DesktopScreen.Services] to [ServicesScreenUi]. */
class ServicesUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Services -> ui<ServicesUiState> { state, modifier ->
            ServicesScreenUi(
                state = state,
                modifier = modifier,
                onNavigate = LocalDesktopNavigate.current,
                isSidebarExpanded = LocalSidebarExpanded.current,
                onToggleSidebar = LocalToggleSidebar.current,
                onProfileClick = LocalProfileClick.current
            )
        }
        else -> null
    }
}
