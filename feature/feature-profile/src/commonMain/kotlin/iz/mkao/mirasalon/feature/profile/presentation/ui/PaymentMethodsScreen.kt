package iz.mkao.mirasalon.feature.profile.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.components.MiraEmptyState
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.domain.model.PaymentMethod
import iz.mkao.mirasalon.core.domain.model.PaymentMethodType
import iz.mkao.mirasalon.feature.profile.presentation.circuit.PaymentMethodsEvent
import iz.mkao.mirasalon.feature.profile.presentation.circuit.PaymentMethodsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreenContent(
    state: PaymentMethodsState,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { MiraTopAppBar(title = "Payment Methods", onBackClick = { state.eventSink(PaymentMethodsEvent.Back) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Outlined.CreditCard, contentDescription = "Add payment method")
            }
        },
    ) { padding ->
        if (state.methods.isEmpty()) {
            MiraEmptyState(
                message = "No payment methods added yet",
                description = "Add a payment method to make your checkout experience faster and easier.",
                icon = Icons.Outlined.CreditCard,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.methods.distinctBy { it.id }, key = { it.id }) { method ->
                    PaymentMethodCard(
                        method = method,
                        onSetDefault = { state.eventSink(PaymentMethodsEvent.SetDefault(method.id)) },
                        onRemove = { state.eventSink(PaymentMethodsEvent.RemoveMethod(method.id)) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddPaymentMethodDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { type, label, last4 ->
                state.eventSink(PaymentMethodsEvent.AddMethod(type, label, last4))
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethod,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(method.type.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.padding(start = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(method.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(method.type.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSetDefault) {
                Icon(
                    if (method.isDefault) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (method.isDefault) "Default method" else "Set as default",
                    tint = if (method.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun PaymentMethodType.icon(): ImageVector = when (this) {
    PaymentMethodType.CASH -> Icons.Outlined.Payments
    PaymentMethodType.CASH_ON_DELIVERY -> Icons.Outlined.LocalShipping
    PaymentMethodType.CARD -> Icons.Outlined.CreditCard
    PaymentMethodType.VISA -> Icons.Outlined.CreditCard
    PaymentMethodType.MASTER_CARD -> Icons.Outlined.CreditCard
    PaymentMethodType.GOOGLE_PAY -> Icons.Outlined.Payments
    PaymentMethodType.APPLE_PAY -> Icons.Outlined.Payments
    PaymentMethodType.RAZORPAY -> Icons.Outlined.Payments
}

@Composable
private fun AddPaymentMethodDialog(
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethodType, String, String?) -> Unit,
) {
    var selectedType by remember { mutableStateOf(PaymentMethodType.CASH) }
    var cardLast4 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add payment method") },
        text = {
            Column {
                PaymentMethodType.entries.forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(
                            onClick = { if (type.isAvailable) selectedType = type },
                            enabled = type.isAvailable,
                        ) {
                            Text(if (type.isAvailable) type.displayName else "${type.displayName} (coming soon)")
                        }
                        if (selectedType == type) {
                            Icon(Icons.Outlined.Star, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (selectedType == PaymentMethodType.CARD) {
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    OutlinedTextField(
                        value = cardLast4,
                        onValueChange = { if (it.length <= 4) cardLast4 = it.filter(Char::isDigit) },
                        label = { Text("Last 4 digits (for display only)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (cardLast4.length == 4) {
                                    val label = "Card ending in $cardLast4"
                                    onConfirm(selectedType, label, cardLast4)
                                }
                            }
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val label = if (selectedType == PaymentMethodType.CARD && cardLast4.length == 4) {
                        "Card ending in $cardLast4"
                    } else {
                        selectedType.displayName
                    }
                    onConfirm(selectedType, label, cardLast4.takeIf { it.length == 4 })
                },
                enabled = selectedType != PaymentMethodType.CARD || cardLast4.length == 4,
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
