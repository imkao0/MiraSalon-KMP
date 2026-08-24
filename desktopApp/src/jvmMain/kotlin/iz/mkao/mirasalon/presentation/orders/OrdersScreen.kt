package iz.mkao.mirasalon.presentation.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraSuccess
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.RadiusSmall
import iz.mkao.mirasalon.core.domain.model.AdminOrder
import iz.mkao.mirasalon.core.domain.model.AdminOrderStatus
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.components.DateFilter
import iz.mkao.mirasalon.presentation.components.DesktopEmptyState
import iz.mkao.mirasalon.presentation.components.DesktopLoadingState
import iz.mkao.mirasalon.presentation.components.DesktopSearchBar
import iz.mkao.mirasalon.presentation.components.DesktopShell
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OrderItemUiModel(val name: String, val price: String, val quantity: Int)

data class OrderUiModel(
    val id: String,
    val clientName: String,
    val orderNumber: String,
    val time: String,
    val shippingInfo: String,
    val itemsCount: Int,
    val total: String,
    val items: List<OrderItemUiModel>,
    val hasMore: Boolean,
    val status: AdminOrderStatus,
)

enum class OrderFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    SHIPPED("Shipped"),
    REFUNDED("Refunded"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
}

@Composable
fun OrdersScreenUi(
    state: OrdersUiState,
    modifier: Modifier = Modifier
) {
    val orders = remember(state.orders) {
        state.orders.map { it.toOrderUiModel() }
            .sortedByDescending { it.status == AdminOrderStatus.Pending }
    }
    
    val filterCounts = remember(state.orders) {
        val counts = mutableMapOf<OrderFilter, Int>()
        counts[OrderFilter.ALL] = state.orders.size
        counts[OrderFilter.PENDING] = state.orders.count { it.status == AdminOrderStatus.Pending }
        counts[OrderFilter.SHIPPED] = state.orders.count { it.status == AdminOrderStatus.Shipped }
        counts[OrderFilter.REFUNDED] = state.orders.count { it.status == AdminOrderStatus.Refunded }
        counts[OrderFilter.DELIVERED] = state.orders.count { it.status == AdminOrderStatus.Delivered }
        counts[OrderFilter.CANCELLED] = state.orders.count { it.status == AdminOrderStatus.Cancelled }
        counts
    }

    val selectedFilter = when (state.selectedStatus) {
        AdminOrderStatus.Pending -> OrderFilter.PENDING
        AdminOrderStatus.Shipped -> OrderFilter.SHIPPED
        AdminOrderStatus.Refunded -> OrderFilter.REFUNDED
        AdminOrderStatus.Delivered -> OrderFilter.DELIVERED
        AdminOrderStatus.Cancelled -> OrderFilter.CANCELLED
        null -> OrderFilter.ALL
    }

    DesktopShell(
        title = "Order Management",
        subtitle = "Track and manage salon orders",
        selectedRoute = "Orders"
    ) {
        FilterRow(
            filterCounts = filterCounts,
            selectedFilter = selectedFilter,
            onFilterSelected = { filter ->
                val status = when (filter) {
                    OrderFilter.ALL -> null
                    OrderFilter.PENDING -> AdminOrderStatus.Pending
                    OrderFilter.SHIPPED -> AdminOrderStatus.Shipped
                    OrderFilter.REFUNDED -> AdminOrderStatus.Refunded
                    OrderFilter.DELIVERED -> AdminOrderStatus.Delivered
                    OrderFilter.CANCELLED -> AdminOrderStatus.Cancelled
                }
                state.eventSink(OrdersEvent.StatusFilterChanged(status))
            },
            searchQuery = state.searchQuery,
            onSearchChange = { state.eventSink(OrdersEvent.Search(it)) },
            dateFilter = state.dateFilter,
            onDateFilterChange = { state.eventSink(OrdersEvent.DateFilterChanged(it)) },
            onTodayClick = { state.eventSink(OrdersEvent.Refresh) },
        )

        Spacer(Modifier.height(26.dp))
        
        if (state.isLoading && state.orders.isEmpty()) {
            DesktopLoadingState(color = MaterialTheme.colorScheme.primary)
        } else if (orders.isEmpty()) {
            DesktopEmptyState(
                icon = Icons.Outlined.ShoppingBag,
                title = "No orders found",
                subtitle = "Try refreshing or changing filters"
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(orders.distinctBy { it.orderNumber }, key = { it.orderNumber }) { order ->
                    OrderCard(
                        order = order,
                        onStatusChange = { newStatus ->
                            state.eventSink(OrdersEvent.UpdateOrderStatus(order.id, newStatus))
                        },
                        onDelete = {
                            state.eventSink(OrdersEvent.DeleteOrder(order.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    filterCounts: Map<OrderFilter, Int>,
    selectedFilter: OrderFilter,
    onFilterSelected: (OrderFilter) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    dateFilter: DateFilter,
    onDateFilterChange: (DateFilter) -> Unit,
    onTodayClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OrderFilter.entries.forEach { filter ->
            val count = filterCounts[filter] ?: 0
            if (count > 0 || filter == OrderFilter.ALL) {
                FilterChip(
                    label = filter.label,
                    count = count,
                    selected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) },
                )
            }
        }

        Spacer(Modifier.weight(1f))

        DesktopSearchBar(
            value = searchQuery,
            onValueChange = onSearchChange,
            width = 200.dp
        )

        var showDateMenu by remember { mutableStateOf(false) }
        Box {
            Row(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                    .clickable { showDateMenu = true }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(text = dateFilter.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Outlined.ArrowDropDown, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }

            DropdownMenu(expanded = showDateMenu, onDismissRequest = { showDateMenu = false }) {
                DateFilter.entries.forEach { filter ->
                    DropdownMenuItem(text = { Text(filter.label) }, onClick = { onDateFilterChange(filter); showDateMenu = false })
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(RadiusSmall)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        count.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(RadiusSmall),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFFF5F5F5),
            labelColor = MiraTextSecondary,
            selectedContainerColor = MiraCoral,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun OrderCard(
    order: OrderUiModel,
    onStatusChange: (AdminOrderStatus) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
        shape = RoundedCornerShape(2.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        shadowElevation = 0.dp
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Status Bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        when (order.status) {
                            AdminOrderStatus.Pending -> MiraCoral
                            AdminOrderStatus.Shipped -> MaterialTheme.colorScheme.tertiary
                            AdminOrderStatus.Delivered -> MiraSuccess
                            AdminOrderStatus.Cancelled -> Color.Gray
                            AdminOrderStatus.Refunded -> Color.LightGray
                        }
                    )
            )

            Column(modifier = Modifier.weight(1f).padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = order.clientName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiraTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = order.orderNumber,
                            fontSize = 12.sp,
                            color = MiraTextSecondary,
                        )
                    }
                    StatusBadge(status = order.status)
                }

                Spacer(Modifier.height(16.dp))
                
                // Meta info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetaRow(icon = Icons.Outlined.Schedule, label = order.time)
                    MetaRow(icon = Icons.Outlined.CalendarToday, label = order.shippingInfo)
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFF9F9F9))
                Spacer(Modifier.height(16.dp))

                // Items list
                Column(modifier = Modifier.weight(1f)) {
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.name} x${item.quantity}",
                                fontSize = 13.sp,
                                color = MiraTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = item.price,
                                fontSize = 13.sp,
                                color = MiraTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (order.hasMore) {
                        Text(
                            text = "+ more items",
                            fontSize = 11.sp,
                            color = MiraCoral,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Footer
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFF9F9F9))
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total", fontSize = 11.sp, color = MiraTextSecondary)
                        Text(text = order.total, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (order.status == AdminOrderStatus.Pending) {
                            Button(
                                onClick = { onStatusChange(AdminOrderStatus.Shipped) },
                                colors = ButtonDefaults.buttonColors(containerColor = MiraCoral),
                                shape = RoundedCornerShape(2.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Ship", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (order.status == AdminOrderStatus.Shipped) {
                            Button(
                                onClick = { onStatusChange(AdminOrderStatus.Delivered) },
                                colors = ButtonDefaults.buttonColors(containerColor = MiraSuccess),
                                shape = RoundedCornerShape(2.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Deliver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.MoreVert, null, tint = MiraTextSecondary, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                if (order.status != AdminOrderStatus.Cancelled && order.status != AdminOrderStatus.Delivered) {
                                    DropdownMenuItem(
                                        text = { Text("Cancel Order") },
                                        onClick = { onStatusChange(AdminOrderStatus.Cancelled); showMenu = false }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete", color = Color.Red) },
                                    onClick = { onDelete(); showMenu = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = MiraTextSecondary, modifier = Modifier.size(14.dp))
        Text(text = label, fontSize = 13.sp, color = MiraTextSecondary)
    }
}

@Composable
private fun StatusBadge(status: AdminOrderStatus) {
    val uiInfo = status.toUiInfo(MaterialTheme.colorScheme)
    Box(
        modifier = Modifier
            .background(uiInfo.bg, RoundedCornerShape(2.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = uiInfo.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = uiInfo.text)
    }
}

private data class StatusUiInfo(val label: String, val bg: Color, val text: Color)

@Composable
private fun AdminOrderStatus.toUiInfo(colorScheme: ColorScheme): StatusUiInfo = when (this) {
    AdminOrderStatus.Pending -> StatusUiInfo("Pending", colorScheme.secondaryContainer, colorScheme.onSecondaryContainer)
    AdminOrderStatus.Shipped -> StatusUiInfo("Shipped", colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer)
    AdminOrderStatus.Refunded -> StatusUiInfo("Refunded", colorScheme.surfaceVariant, colorScheme.onSurfaceVariant)
    AdminOrderStatus.Delivered -> StatusUiInfo("Delivered", colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
    AdminOrderStatus.Cancelled -> StatusUiInfo("Cancelled", colorScheme.outlineVariant, colorScheme.onSurfaceVariant)
}

private fun Modifier.clickableSurface(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

fun AdminOrder.toOrderUiModel(): OrderUiModel {
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val mappedItems = items.map { 
        val lineTotal = it.unitPrice * it.quantity
        OrderItemUiModel(it.productName, "$${"%,.2f".format(lineTotal)}", it.quantity) 
    }
    
    if (mappedItems.isEmpty() && totalAmount > 0) {
        Napier.w(tag = "OrdersScreen") { "Order $id has totalAmount $totalAmount but 0 items. Raw items count: ${items.size}" }
    }
    
    return OrderUiModel(
        id = id,
        clientName = customerName,
        orderNumber = "#${id.take(8).uppercase()}",
        time = timeFormatter.format(Date(createdAt)),
        shippingInfo = "Delivery",
        itemsCount = items.sumOf { it.quantity },
        total = "$${"%,.2f".format(totalAmount)}",
        items = mappedItems.take(3),
        hasMore = items.size > 3,
        status = status
    )
}

/** Circuit [Ui.Factory] binding [DesktopScreen.Orders] to [OrdersScreenUi]. */
class OrdersUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Orders -> ui<OrdersUiState> { state, modifier ->
            OrdersScreenUi(
                state = state,
                modifier = modifier
            )
        }
        else -> null
    }
}
