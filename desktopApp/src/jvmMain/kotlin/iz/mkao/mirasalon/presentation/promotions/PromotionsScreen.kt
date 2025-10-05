package iz.mkao.mirasalon.presentation.promotions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.VelvetaSlateBlue
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.domain.model.AdminDiscountType
import iz.mkao.mirasalon.core.domain.model.AdminPromoStatus
import iz.mkao.mirasalon.core.domain.model.AdminPromotion
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.LocalDesktopNavigate
import iz.mkao.mirasalon.presentation.LocalProfileClick
import iz.mkao.mirasalon.presentation.LocalSidebarExpanded
import iz.mkao.mirasalon.presentation.LocalToggleSidebar
import iz.mkao.mirasalon.presentation.components.DesktopLoadingState
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardHeader
import iz.mkao.mirasalon.presentation.dashboard.components.Sidebar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PromotionsScreenUi(
    state: PromotionsUiState,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    isSidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    onProfileClick: () -> Unit
) {
    val promotions = state.promotions
    val isLoading = state.isLoading

    var showAddDialog by remember { mutableStateOf(false) }
    var editingPromotion by remember { mutableStateOf<AdminPromotion?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showAddDialog || editingPromotion != null) {
        PromotionDialog(
            promotion = editingPromotion,
            services = state.services,
            productCategories = state.productCategories,
            serviceCategories = state.serviceCategories,
            onDismiss = {
                showAddDialog = false
                editingPromotion = null
            },
            onConfirm = { promotion ->
                if (editingPromotion != null) {
                    state.eventSink(PromotionsEvent.UpdatePromotion(promotion))
                } else {
                    state.eventSink(PromotionsEvent.CreatePromotion(promotion))
                }
                showAddDialog = false
                editingPromotion = null
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Promotions") },
            text = { Text("Are you sure you want to delete ALL promotions? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        state.eventSink(PromotionsEvent.ClearAll)
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Row(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Sidebar(
            isExpanded = isSidebarExpanded,
            onToggle = onToggleSidebar,
            selectedRoute = "Promotions",
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxHeight().width(if (isSidebarExpanded) 280.dp else 80.dp)
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Promotions & Offers",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MiraTextPrimary
                    )

                    Spacer(modifier = Modifier.width(32.dp))

            
                    Surface(
                        modifier = Modifier.width(400.dp).height(40.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, MiraBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp), tint = MiraTextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { state.eventSink(PromotionsEvent.Search(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = MiraTextPrimary),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (state.searchQuery.isEmpty()) {
                                        Text("Search promotions...", fontSize = 14.sp, color = MiraTextSecondary)
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick() },
                    shape = CircleShape,
                    color = Color.LightGray
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Current Offers (${promotions.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MiraTextPrimary
                )

                Row {
                    if (promotions.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Icon(Icons.Outlined.DeleteSweep, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All")
                        }
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MiraCoral),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Icon(Icons.Outlined.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Promotion")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading && promotions.isEmpty()) {
                DesktopLoadingState()
            } else if (promotions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.LocalOffer,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No promotions found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MiraTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Create your first offer to attract more customers!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MiraTextSecondary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MiraCoral),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Icon(Icons.Outlined.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Promotion")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(promotions) { promo ->
                        PromotionListItem(
                            promotion = promo,
                            onToggleActive = { /* state.eventSink(PromotionsEvent.UpdatePromotion(promo.copy(...))) */ },
                            onEdit = { editingPromotion = promo },
                            onDelete = { state.eventSink(PromotionsEvent.DeletePromotion(promo.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionListItem(
    promotion: AdminPromotion,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        shape = RoundedCornerShape(2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MiraBorder),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageUrl = promotion.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                val fullUrl = ApiEndpoints.resolveImageUrl(imageUrl)
                AsyncImage(
                    model = fullUrl,
                    contentDescription = promotion.code,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = {
                        Napier.e(it.result.throwable) { "Coil failed to load promotion image: $fullUrl" }
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White).border(1.dp, MiraBorder, RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.LocalOffer, null, tint = MiraCoral.copy(alpha = 0.2f), modifier = Modifier.size(80.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.05f),
                                Color.White.copy(alpha = 0.9f),
                                Color.White
                            )
                        )
                    )
            )

            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                color = if (promotion.status == AdminPromoStatus.Active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    promotion.status.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (promotion.status == AdminPromoStatus.Active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            promotion.code,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MiraTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            promotion.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MiraTextSecondary,
                            maxLines = 1
                        )
                    }
                    Text(
                        if (promotion.discountType == AdminDiscountType.Percentage) "${promotion.discountValue.toInt()}% OFF" else "$${promotion.discountValue} OFF",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MiraCoral
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MiraBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, null, tint = MiraTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${dateFormatter.format(Date(promotion.validFrom))} - ${promotion.validUntil?.let { dateFormatter.format(Date(it)) } ?: "Never"}",
                            fontSize = 11.sp,
                            color = MiraTextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${promotion.currentUsageCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MiraTextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Redeemed", style = MaterialTheme.typography.labelSmall, color = MiraTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Min Spend: $${promotion.minOrderValue ?: 0.0}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiraTextSecondary
                    )

                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Edit, null, tint = VelvetaSlateBlue, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onToggleActive, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (promotion.status == AdminPromoStatus.Active) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                                null,
                                tint = VelvetaSlateBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Delete, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Circuit [Ui.Factory] binding [DesktopScreen.Promotions] to [PromotionsScreenUi]. */
class PromotionsUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Promotions -> ui<PromotionsUiState> { state, modifier ->
            PromotionsScreenUi(
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
