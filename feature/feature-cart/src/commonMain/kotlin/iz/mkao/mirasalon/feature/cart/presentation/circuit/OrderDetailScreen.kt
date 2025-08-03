package iz.mkao.mirasalon.feature.cart.presentation.circuit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.common.util.toPriceString
import iz.mkao.mirasalon.core.designsystem.components.MiraButton
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.domain.model.Order

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailContent(
    state: OrderDetailState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MiraTopAppBar(
                title = "Order Details",
                onBackClick = { state.eventSink(OrderDetailEvent.Back) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> {
                    ShimmerLoading(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(state.error, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
                }
                state.order != null -> {
                    OrderDetailBody(
                        order = state.order,
                        fromCheckout = state.fromCheckout,
                        placedAt = state.placedAt,
                        onHomeClick = { state.eventSink(OrderDetailEvent.Home) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailBody(
    order: Order,
    fromCheckout: Boolean,
    placedAt: String,
    onHomeClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (fromCheckout) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        text = "Order Placed Successfully!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "Thank you for your purchase.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    MiraButton(
                        text = "Go to Home",
                        onClick = onHomeClick,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }
            }
        }

        item {
            OrderDetailSection(title = "Order Info") {
                DetailRow("Order ID", order.id.takeLast(4).uppercase())
                DetailRow("Status", order.status.name)
                DetailRow("Placed At", placedAt)
            }
        }

        item {
            OrderDetailSection(title = "Customer Info") {
                DetailRow("Customer ID", order.userId.takeLast(4).uppercase())
                DetailRow("Name", order.userName)
                DetailRow("Email", order.userEmail)
                order.userPhone?.let { DetailRow("Phone", it) }
            }
        }

        item {
            OrderDetailSection(title = "Items") {
                order.items.forEach { item ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.product.name} x${item.quantity}", modifier = Modifier.weight(1f))
                            Text((item.product.discountedPrice * item.quantity).toPriceString())
                        }
                        Text("SKU: ${item.product.id.takeLast(4).uppercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            OrderDetailSection(title = "Payment") {
                DetailRow("Subtotal", order.subtotal.toPriceString())
                DetailRow("Shipping Fees", order.shippingFees.toPriceString())
                DetailRow("Taxes", order.tax.toPriceString())
                if (order.discount > 0) {
                    DetailRow("Discount", "-${order.discount.toPriceString()}")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", fontWeight = FontWeight.Bold)
                    Text(order.total.toPriceString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                order.paymentMethod?.let { DetailRow("Payment Method", it) }
                order.shippingAddress?.let { DetailRow("Shipping Address", it) }
            }
        }

        item {
            OrderDetailSection(title = "Fulfillment") {
                order.trackingNumber?.let { DetailRow("Tracking Number", it) }
                order.specialInstructions?.let { DetailRow("Special Instructions", it) }
            }
        }
    }
}

@Composable
private fun OrderDetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
