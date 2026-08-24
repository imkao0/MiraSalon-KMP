package iz.mkao.mirasalon.feature.cart.presentation.circuit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.common.util.toPriceString
import iz.mkao.mirasalon.core.designsystem.components.MiraEmptyState
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraSuccess
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.core.designsystem.theme.RadiusSmall
import iz.mkao.mirasalon.core.domain.model.OrderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersContent(
    state: OrdersState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MiraTopAppBar(
                title = "Order history",
                onBackClick = { state.eventSink(OrdersEvent.Back) }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ShimmerLoading()
            }
        } else if (state.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { state.eventSink(OrdersEvent.Retry) }) {
                        Text("Retry")
                    }
                }
            }
        } else if (state.orders.isEmpty()) {
            MiraEmptyState(
                message = "You have no orders yet.",
                description = "Once you place an order, it will appear here in your order history.",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.orders.distinctBy { it.order.id }, key = { it.order.id }) { orderUi ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                state.eventSink(OrdersEvent.RemoveOrder(orderUi.order.id))
                                true
                            } else {
                                false
                            }
                        }
                    )
                    
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(RadiusSmall))
                                    .background(Color.Red.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    tint = Color.Red
                                )
                            }
                        }
                    ) {
                        OrderCard(orderUi = orderUi, onClick = { state.eventSink(OrdersEvent.OrderClicked(orderUi.order.id)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(orderUi: OrderUiModel, onClick: () -> Unit) {
    val order = orderUi.order
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(2.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        shadowElevation = 0.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left Status Bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        when (order.status) {
                            OrderStatus.PENDING -> MiraCoral
                            OrderStatus.SHIPPED -> MaterialTheme.colorScheme.tertiary
                            OrderStatus.DELIVERED -> MiraSuccess
                            OrderStatus.CANCELLED -> Color.Gray
                            OrderStatus.REFUNDED -> Color.LightGray
                        }
                    )
            )

            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = order.userName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiraTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "#${order.id.take(8).uppercase()}",
                            fontSize = 12.sp,
                            color = MiraTextSecondary,
                        )
                    }
                    StatusBadge(status = order.status)
                }

                Spacer(Modifier.height(12.dp))

                // Meta info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetaRow(icon = Icons.Outlined.Schedule, label = orderUi.formattedDate)
                    MetaRow(icon = Icons.Outlined.CalendarToday, label = if (order.shippingAddress != null) "Delivery" else "Pickup")
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFF9F9F9))
                Spacer(Modifier.height(12.dp))

                // Items list
                Column {
                    order.items.take(3).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.product.name} x${item.quantity}",
                                fontSize = 13.sp,
                                color = MiraTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.product.discountPercent > 0) {
                                    Text(
                                        text = (item.product.price * item.quantity).toPriceString(),
                                        fontSize = 11.sp,
                                        color = MiraTextSecondary,
                                        textDecoration = TextDecoration.LineThrough,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                                Text(
                                    text = (item.product.discountedPrice * item.quantity).toPriceString(),
                                    fontSize = 13.sp,
                                    color = MiraTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    if (order.items.size > 3) {
                        Text(
                            text = "+ ${order.items.size - 3} more items",
                            fontSize = 11.sp,
                            color = MiraCoral,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFF9F9F9))
                Spacer(Modifier.height(12.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total", fontSize = 11.sp, color = MiraTextSecondary)
                        Text(text = order.total.toPriceString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    }

                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MiraCoral.copy(alpha = 0.1f),
                            contentColor = MiraCoral
                        ),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
private fun StatusBadge(status: OrderStatus) {
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
private fun OrderStatus.toUiInfo(colorScheme: ColorScheme): StatusUiInfo = when (this) {
    OrderStatus.PENDING -> StatusUiInfo("Pending", colorScheme.secondaryContainer, colorScheme.onSecondaryContainer)
    OrderStatus.SHIPPED -> StatusUiInfo("Shipped", colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer)
    OrderStatus.REFUNDED -> StatusUiInfo("Refunded", colorScheme.surfaceVariant, colorScheme.onSurfaceVariant)
    OrderStatus.DELIVERED -> StatusUiInfo("Delivered", colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
    OrderStatus.CANCELLED -> StatusUiInfo("Cancelled", colorScheme.outlineVariant, colorScheme.onSurfaceVariant)
}

