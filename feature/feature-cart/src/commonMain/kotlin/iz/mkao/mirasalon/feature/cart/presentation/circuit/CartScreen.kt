package iz.mkao.mirasalon.feature.cart.presentation.circuit

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import iz.mkao.mirasalon.core.common.util.toPriceString
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.domain.model.CartItem
import iz.mkao.mirasalon.core.domain.model.Order
import iz.mkao.mirasalon.core.navigation.BottomNavKey
import iz.mkao.mirasalon.core.navigation.CartRoute
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.feature.cart.checkout.presentation.circuit.CheckoutContent
import iz.mkao.mirasalon.feature.cart.checkout.presentation.circuit.CheckoutState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartContent(
    state: CartState,
    modifier: Modifier = Modifier
) {
    val groupedItems = remember(state.cart.items) {
        state.cart.items.groupBy { it.product.providerName }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MiraTopAppBar(
                title = "Cart (${state.cart.itemCount})",
                onBackClick = { state.eventSink(CartEvent.Back) }
            )
        },
        bottomBar = {
            if (state.cart.items.isNotEmpty()) {
                CartBottomBar(
                    state = state,
                    onCheckout = { state.eventSink(CartEvent.Checkout) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (state.cart.items.isEmpty() && state.expiredOrders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Your cart is empty", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            groupedItems.forEach { (storeName, items) ->
                item(key = "store_$storeName") {
                    StoreHeader(
                        name = storeName,
                        isSelected = items.all { state.selectedItemIds.contains(it.product.id) },
                        onToggle = { isSelected ->
                            state.eventSink(CartEvent.ToggleStoreSelection(storeName, isSelected))
                        }
                    )
                }

                items(items, key = { it.product.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                state.eventSink(CartEvent.RemoveItem(item.product.id))
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
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
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
                        CartItemRow(
                            item = item,
                            isSelected = state.selectedItemIds.contains(item.product.id),
                            onToggle = { state.eventSink(CartEvent.ToggleSelection(item.product.id)) },
                            onUpdateQuantity = { qty ->
                                state.eventSink(CartEvent.UpdateQuantity(item.product.id, qty))
                            }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            if (state.expiredCartItems.isNotEmpty()) {
                item {
                    Text(
                        "Expired Items",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(state.expiredCartItems, key = { "cart_${it.product.id}" }) { item ->
                    CartItemRow(
                        item = item,
                        isSelected = false,
                        onToggle = {},
                        onUpdateQuantity = {},
                        isExpired = true
                    )
                }
            }

            if (state.expiredOrders.isNotEmpty()) {
                item {
                    Text(
                        "Recent Cancelled Orders",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(state.expiredOrders, key = { it.id }) { order ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                state.eventSink(CartEvent.RemoveExpiredOrder(order.id))
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
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
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
                        ExpiredOrderCard(order = order)
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreHeader(
    name: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularCheckbox(
            checked = isSelected,
            onCheckedChange = onToggle,
            activeColor = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Default.Storefront,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onUpdateQuantity: (Int) -> Unit,
    isExpired: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isExpired) Color.White.copy(alpha = 0.6f) else Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isExpired) {
                CircularCheckbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    activeColor = Color.LightGray
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                // Spacer to align with items that have checkboxes
                Box(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            AsyncImage(
                model = ApiEndpoints.resolveImageUrl(item.product.imageUrl),
                contentDescription = item.product.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isExpired) Color.Gray else Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
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
                        text = item.product.discountedPrice.toPriceString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    if (isExpired) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = "EXPIRED",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    } else {
                        QuantityControl(
                            quantity = item.quantity,
                            onDecrease = { if (item.quantity > 1) onUpdateQuantity(item.quantity - 1) },
                            onIncrease = { onUpdateQuantity(item.quantity + 1) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiredOrderCard(
    order: Order
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularCheckbox(
                    checked = true,
                    onCheckedChange = {},
                    activeColor = Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.items.firstOrNull()?.product?.providerName ?: "Mira Store",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Placeholder for Checkbox space to maintain alignment
                    Box(modifier = Modifier.size(22.dp))

                    Spacer(modifier = Modifier.width(12.dp))

                    AsyncImage(
                        model = ApiEndpoints.resolveImageUrl(item.product.imageUrl),
                        contentDescription = item.product.name,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.product.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.product.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.product.discountedPrice.toPriceString(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                )
                                Text(
                                    text = "Qty: ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text(
                                    text = "EXPIRED",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
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
private fun QuantityControl(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDecrease, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Outlined.Remove, null, modifier = Modifier.size(14.dp))
        }
        Text(
            text = quantity.toString(),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(onClick = onIncrease, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun CircularCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (checked) activeColor else Color.Transparent)
            .border(1.dp, if (checked) activeColor else Color.LightGray, CircleShape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun CartBottomBar(
    state: CartState,
    onCheckout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Promo Code Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.promoCode,
                    onValueChange = { state.eventSink(CartEvent.PromoCodeChanged(it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Promo Code", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    isError = state.error != null,
                    trailingIcon = {
                        if (state.cart.couponCode != null) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Applied",
                                tint = Color.Green,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (state.cart.couponCode != null) {
                            state.eventSink(CartEvent.RemoveCoupon)
                        } else {
                            state.eventSink(CartEvent.ApplyCoupon(state.promoCode))
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.cart.couponCode != null) Color.Gray else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (state.cart.couponCode != null) "Remove" else "Apply")
                }
            }

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Price Breakdown
            if (state.cart.discountAmount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                    Text(state.cart.subtotal.toPriceString("US $"), style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Discount", style = MaterialTheme.typography.bodyMedium, color = Color.Green)
                    Text("- ${state.cart.discountAmount.toPriceString("US $")}", style = MaterialTheme.typography.bodyMedium, color = Color.Green)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = state.cart.total.toPriceString("US $"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = onCheckout,
                    modifier = Modifier
                        .height(50.dp)
                        .width(180.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "CHECKOUT",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

class CartManualUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is CartRoute.Cart -> ui<CartState> { state, modifier -> CartContent(state, modifier) }
            is BottomNavKey.Cart -> ui<CartState> { state, modifier -> CartContent(state, modifier) }
            is CartRoute.Checkout -> ui<CheckoutState> { state, modifier -> CheckoutContent(state, modifier) }
            is CartRoute.Orders -> ui<OrdersState> { state, modifier -> OrdersContent(state, modifier) }
            is CartRoute.OrderDetail -> ui<OrderDetailState> { state, modifier -> OrderDetailContent(state, modifier) }
            is CartRoute.PaymentSuccess -> ui<CartPaymentSuccessState> { state, modifier -> PaymentSuccessContent(state, modifier) }
            else -> null
        }
    }
}
