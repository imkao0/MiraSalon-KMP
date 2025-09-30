package iz.mkao.mirasalon.presentation.customers

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Shortcut
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import iz.mkao.mirasalon.core.common.util.ChatUtils
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.core.designsystem.theme.MiraBorder
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraGreen
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.MiraYellow
import iz.mkao.mirasalon.core.designsystem.theme.RadiusMedium
import iz.mkao.mirasalon.core.domain.model.AdminOrder
import iz.mkao.mirasalon.core.domain.model.AdminOrderStatus
import iz.mkao.mirasalon.core.domain.model.AdminReview
import iz.mkao.mirasalon.core.domain.model.AppointmentDailyPoint
import iz.mkao.mirasalon.core.domain.model.CustomerDetail
import iz.mkao.mirasalon.core.domain.model.CustomerSummary
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.LocalDesktopNavigate
import iz.mkao.mirasalon.presentation.components.DesktopEmptyState
import iz.mkao.mirasalon.presentation.components.DesktopLoadingState
import iz.mkao.mirasalon.presentation.components.DesktopPrimaryButton
import iz.mkao.mirasalon.presentation.components.DesktopSearchBar
import iz.mkao.mirasalon.presentation.components.DesktopShell
import iz.mkao.mirasalon.presentation.dashboard.charts.SalesLineChart
import iz.mkao.mirasalon.presentation.dashboard.components.ChartLegendItem
import iz.mkao.mirasalon.presentation.dashboard.components.DashboardCard
import java.util.Locale

@Composable
fun CustomersScreenUi(
    state: CustomersUiState,
    modifier: Modifier = Modifier
) {
    val onNavigate = LocalDesktopNavigate.current
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        CustomerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, email ->
                state.eventSink(CustomersEvent.CreateCustomer(name, email))
                showAddDialog = false
            }
        )
    }

    state.selectedCustomer?.let { customer ->
        CustomerDetailDialog(
            customer = customer,
            onDismiss = { state.eventSink(CustomersEvent.SelectCustomer(null)) },
            onUpdate = { name, email, avatar ->
                state.eventSink(CustomersEvent.UpdateCustomer(customer.id, name, email, avatar))
            },
            onDelete = {
                state.eventSink(CustomersEvent.DeleteCustomer(customer.id))
                state.eventSink(CustomersEvent.SelectCustomer(null))
            }
        )
    }

    state.showReviews?.let { customer ->
        CustomerReviewsDialog(
            customer = customer,
            reviews = state.customerReviews,
            isLoading = state.isLoading,
            onDismiss = { state.eventSink(CustomersEvent.ShowReviews(null)) }
        )
    }

    state.showSales?.let { customer ->
        CustomerSalesDialog(
            customer = customer,
            sales = state.customerSales,
            isLoading = state.isLoading,
            onDismiss = { state.eventSink(CustomersEvent.ShowSales(null)) }
        )
    }

    DesktopShell(
        title = "Customer",
        subtitle = null, // Moved to metrics
        selectedRoute = "Customers"
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Metrics Row
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    CustomerMetricCard(
                        title = "Total",
                        value = state.totalCustomers.toString(),
                        change = "45%",
                        modifier = Modifier.weight(1f)
                    )
                    CustomerMetricCard(
                        title = "New customer",
                        value = state.newCustomers.toString(),
                        change = "45%",
                        modifier = Modifier.weight(1f)
                    )
                    CustomerMetricCard(
                        title = "Return customer rate",
                        value = "${state.returnCustomerRate.toInt()}%",
                        change = "45%",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Activity Graph
            item(span = { GridItemSpan(maxLineSpan) }) {
                ActivitySection(
                    points = state.activityPoints,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }

            // Search and Actions Bar
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DesktopSearchBar(
                        value = state.searchQuery,
                        onValueChange = { state.eventSink(CustomersEvent.Search(it)) },
                        placeholder = "Search"
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { state.eventSink(CustomersEvent.Filter) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MiraBorder),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Outlined.FilterList, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Filters", color = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = { state.eventSink(CustomersEvent.Export) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MiraBorder),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Export", color = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Outlined.Shortcut, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        DesktopPrimaryButton(
                            text = "Add Customer",
                            onClick = { showAddDialog = true },
                            icon = Icons.Outlined.PersonAdd
                        )
                    }
                }
            }

            // Customer List Items
            if (state.isLoading && state.customers.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DesktopLoadingState()
                }
            } else if (state.customers.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DesktopEmptyState(
                        icon = Icons.Outlined.Person,
                        title = "No customers found",
                        subtitle = "Try adjusting your search query or add a new customer."
                    )
                }
            } else {
                items(state.customers, key = { it.id }) { customer ->
                    CustomerListItem(
                        customer = customer,
                        onClick = { state.eventSink(CustomersEvent.SelectCustomer(customer.id)) },
                        onReviewsClick = { state.eventSink(CustomersEvent.ShowReviews(customer.id)) },
                        onSalesClick = { state.eventSink(CustomersEvent.ShowSales(customer.id)) },
                        onMessageClick = {
                            // Default to admin-customer chat if no specific specialist context
                            val sessionId = ChatUtils.getDeterministicChatId("admin", customer.id)
                            onNavigate("Chat/$sessionId")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerMetricCard(
    title: String,
    value: String,
    change: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MiraBorder.copy(alpha = 0.5f)),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = MiraTextSecondary, fontSize = 14.sp)
                Icon(Icons.Outlined.Info, null, tint = MiraBorder, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MiraTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.Shortcut, null, modifier = Modifier.size(14.dp), tint = MiraGreen)
                Spacer(modifier = Modifier.width(4.dp))
                Text(change, color = MiraGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text("from last month", color = MiraTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ActivitySection(
    points: List<AppointmentDailyPoint>,
    modifier: Modifier = Modifier
) {
    DashboardCard(
        modifier = modifier,
        title = "Activity",
        subtitle = "Track for bookings",
        timeLabel = "7days"
    ) {
        Column {
            val totalConfirmed = points.sumOf { it.confirmed }
            val totalCancelled = points.sumOf { it.cancelled }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Confirmed ", color = MiraTextSecondary, fontSize = 14.sp)
                Text(totalConfirmed.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Cancelled ", color = MiraTextSecondary, fontSize = 14.sp)
                Text(totalCancelled.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (points.isNotEmpty()) {
                val primaryData = points.map { it.confirmed.toFloat() }
                val secondaryData = points.map { it.cancelled.toFloat() }
                
                SalesLineChart(
                    primaryData = primaryData,
                    secondaryData = secondaryData,
                    dayLabels = points.map { it.date },
                    dayStartIndices = points.indices.toList(),
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    maxValue = (points.maxOf { it.confirmed.coerceAtLeast(it.cancelled) }.toFloat() * 1.2f).coerceAtLeast(5f),
                    yAxisLabel = "Number of appointment",
                    primaryColor = MiraYellow,
                    secondaryColor = MiraCoral,
                    isCurrency = false
                )
            } else {
                Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                    Text("No activity data available", color = MiraTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ChartLegendItem(MiraYellow, "Confirmed", hasInnerDot = true)
                Spacer(modifier = Modifier.width(16.dp))
                ChartLegendItem(MiraCoral, "Cancelled", hasInnerDot = true)
            }
        }
    }
}

@Composable
fun CustomerListItem(
    customer: CustomerSummary,
    onClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onSalesClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(RadiusMedium),
        color = Color.White,
        border = BorderStroke(1.dp, MiraBorder)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, MiraBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MiraTextSecondary
                    )
                    customer.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                        val fullUrl = ApiEndpoints.resolveImageUrl(imageUrl)
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = customer.name,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            onError = {
                                Napier.e(it.result.throwable) { "Coil failed to load customer image: $fullUrl" }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MiraTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Customer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MiraTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MiraBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            CustomerDetailRow(icon = Icons.Outlined.Email, text = customer.email)
            CustomerDetailRow(icon = Icons.Outlined.Phone, text = if (customer.phone.isBlank()) "No phone number" else customer.phone)
            CustomerDetailRow(icon = Icons.AutoMirrored.Outlined.Chat, text = "Message", onClick = onMessageClick)
            CustomerDetailRow(icon = Icons.Outlined.StarOutline, text = "Reviews", onClick = onReviewsClick)
            CustomerDetailRow(icon = Icons.Outlined.Badge, text = "Sales", onClick = onSalesClick)
        }
    }
}

@Composable
fun CustomerReviewsDialog(
    customer: CustomerDetail,
    reviews: List<AdminReview>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reviews from ", style = MaterialTheme.typography.titleLarge)
                Text(customer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MiraCoral)
            }
        },
        text = {
            Column(modifier = Modifier.width(600.dp).heightIn(max = 600.dp)) {
                if (isLoading && reviews.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        DesktopLoadingState()
                    }
                } else if (reviews.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No reviews yet", color = MiraTextSecondary)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(reviews) { review ->
                            CustomerReviewItem(review)
                            HorizontalDivider(color = MiraBorder.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MiraCoral)
            }
        }
    )
}

@Composable
private fun CustomerReviewItem(review: AdminReview) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (index < review.rating) MiraYellow else MiraBorder,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = review.rating.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Text(
                text = DateUtils.formatDateFull(review.createdAt),
                color = MiraTextSecondary,
                fontSize = 12.sp
            )
        }
        
        review.targetName?.let { target ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Service: $target",
                style = MaterialTheme.typography.bodySmall,
                color = MiraCoral,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = review.comment ?: "No comment provided",
            style = MaterialTheme.typography.bodyMedium,
            color = MiraTextPrimary
        )
        
        if (review.adminReply != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MiraBorder.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Admin Reply",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MiraTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = review.adminReply!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MiraTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerSalesDialog(
    customer: CustomerDetail,
    sales: List<AdminOrder>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Orders for ", style = MaterialTheme.typography.titleLarge)
                Text(customer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MiraCoral)
            }
        },
        text = {
            Column(modifier = Modifier.width(800.dp).heightIn(max = 600.dp)) {
                if (isLoading && sales.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        DesktopLoadingState()
                    }
                } else if (sales.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No orders found", color = MiraTextSecondary)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(sales) { order ->
                            CustomerOrderItem(order)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MiraCoral)
            }
        }
    )
}

@Composable
private fun CustomerOrderItem(order: AdminOrder) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MiraBorder.copy(alpha = 0.5f)),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Order #${order.id.takeLast(8).uppercase()}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = DateUtils.formatDateTime(order.createdAt),
                        color = MiraTextSecondary,
                        fontSize = 12.sp
                    )
                }
                
                OrderStatusBadge(order.status)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.quantity}x ${item.productName}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", item.unitPrice * item.quantity)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MiraBorder.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment: ${order.paymentMethod.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiraTextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleSmall,
                        color = MiraTextSecondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MiraCoral
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderStatusBadge(status: AdminOrderStatus) {
    val color = when (status) {
        AdminOrderStatus.Pending -> MiraYellow
        AdminOrderStatus.Delivered -> MiraGreen
        AdminOrderStatus.Cancelled -> MiraCoral
        AdminOrderStatus.Shipped -> Color(0xFF2196F3)
        AdminOrderStatus.Refunded -> Color(0xFF9C27B0)
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = status.name,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CustomerDetailRow(
    icon: ImageVector,
    text: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MiraTextSecondary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MiraTextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MiraTextSecondary
            )
        }
    }
}


@Composable
fun CustomerDetailDialog(
    customer: CustomerDetail,
    onDismiss: () -> Unit,
    onUpdate: (String, String, String?) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var email by remember { mutableStateOf(customer.email) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customer Profile") },
        text = {
            Column(modifier = Modifier.width(400.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(name, email, customer.imageUrl); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = MiraCoral)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MiraCoral)) {
                Text("Delete Customer")
            }
        }
    )
}

@Composable
fun CustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Customer") },
        text = {
            Column(modifier = Modifier.width(400.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email) },
                colors = ButtonDefaults.buttonColors(containerColor = MiraCoral),
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/** Circuit [Ui.Factory] binding [DesktopScreen.Customers] to [CustomersScreenUi]. */
class CustomersUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Customers -> ui<CustomersUiState> { state, modifier ->
            CustomersScreenUi(
                state = state,
                modifier = modifier
            )
        }
        else -> null
    }
}
