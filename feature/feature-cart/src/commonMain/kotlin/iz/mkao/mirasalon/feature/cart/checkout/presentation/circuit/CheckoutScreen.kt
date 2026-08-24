package iz.mkao.mirasalon.feature.cart.checkout.presentation.circuit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.common.util.toPriceString
import iz.mkao.mirasalon.core.designsystem.Res
import iz.mkao.mirasalon.core.designsystem.applepay
import iz.mkao.mirasalon.core.designsystem.gp
import iz.mkao.mirasalon.core.designsystem.mv
import iz.mkao.mirasalon.core.domain.model.Address
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutContent(
    state: CheckoutState,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column {
                CheckoutHeader(
                    onCancel = { state.eventSink(CheckoutEvent.Back) }
                )
                CheckoutStepper(currentStep = state.currentStep)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            }
        },
        bottomBar = {
            val buttonText = when (state.currentStep) {
                CheckoutStep.Shipping -> "Proceed to Payment"
                CheckoutStep.Payment -> "Confirm and continue"
                CheckoutStep.Review -> if (state.isPlacingOrder) "Submitting..." else "Submit Order"
            }
            val onBottomClick = {
                when (state.currentStep) {
                    CheckoutStep.Shipping -> state.eventSink(CheckoutEvent.ProceedToPayment)
                    CheckoutStep.Payment -> state.eventSink(CheckoutEvent.ProceedToReview)
                    CheckoutStep.Review -> state.eventSink(CheckoutEvent.PlaceOrder)
                }
            }
            val isShippingReady = state.selectedAddress != null ||
                (state.addressInput.street.isNotBlank() &&
                 state.addressInput.city.isNotBlank() &&
                 state.addressInput.state.isNotBlank() &&
                 state.addressInput.zipCode.isNotBlank() &&
                 state.addressInput.country.isNotBlank())
            CheckoutBottomBar(
                buttonText = buttonText,
                onClick = onBottomClick,
                enabled = !state.isPlacingOrder && 
                          !state.hasOutOfStockItems &&
                          (state.currentStep != CheckoutStep.Shipping || isShippingReady)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState.index > initialState.index) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                }
            ) { step ->
                when (step) {
                    CheckoutStep.Shipping -> ShippingStep(state)
                    CheckoutStep.Payment -> PaymentStep(state)
                    CheckoutStep.Review -> ReviewStep(state)
                }
            }
        }
    }

    if (state.showAddCardSheet) {
        ModalBottomSheet(
            onDismissRequest = { state.eventSink(CheckoutEvent.ToggleAddCardSheet) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        ) {
            AddCardSheet(state)
        }
    }
}

@Composable
private fun ShippingStep(state: CheckoutState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Shipping address",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter your shipping address for delivery.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
                if (state.error != null && !state.showAddressForm) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error,
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }


        if (state.addresses.isNotEmpty()) {
            items(state.addresses.size) { index ->
                val address = state.addresses[index]
                AddressCard(
                    address = address,
                    isSelected = state.selectedAddress?.id == address.id,
                    onClick = { state.eventSink(CheckoutEvent.AddressSelected(address)) }
                )
            }
        }

        item {
            Button(
                onClick = { state.eventSink(CheckoutEvent.ToggleAddressForm) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = if (state.showAddressForm) "Cancel" else "+ Add new address",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (state.showAddressForm) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CardField(
                        label = "Street",
                        value = state.addressInput.street,
                        onValueChange = { state.eventSink(CheckoutEvent.StreetChanged(it)) },
                        placeholder = "123 Main St",
                        imeAction = ImeAction.Next
                    )

                    CardField(
                        label = "City",
                        value = state.addressInput.city,
                        onValueChange = { state.eventSink(CheckoutEvent.CityChanged(it)) },
                        placeholder = "New York",
                        imeAction = ImeAction.Next
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CardField(
                            label = "State",
                            value = state.addressInput.state,
                            onValueChange = { state.eventSink(CheckoutEvent.StateChanged(it)) },
                            placeholder = "NY",
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next
                        )
                        CardField(
                            label = "ZIP Code",
                            value = state.addressInput.zipCode,
                            onValueChange = { state.eventSink(CheckoutEvent.ZipCodeChanged(it)) },
                            placeholder = "10001",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next
                        )
                    }

                    CardField(
                        label = "Country",
                        value = state.addressInput.country,
                        onValueChange = { state.eventSink(CheckoutEvent.CountryChanged(it)) },
                        placeholder = "USA",
                        imeAction = ImeAction.Done
                    )

                    if (state.error != null) {
                        Text(
                            text = state.error,
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressCard(
    address: Address,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (address.name.isNotBlank()) {
                Text(
                    text = address.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = address.city,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = address.street,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${address.state}, ${address.zipCode}, ${address.country}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PaymentStep(state: CheckoutState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Choose a payment method",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please select a payment method most convenient to you.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            CreditCardOptionRow(
                selected = state.selectedPaymentMethod == "Credit Card",
                onClick = { state.eventSink(CheckoutEvent.PaymentMethodSelected("Credit Card")) }
            )
        }

        item {
            WalletButton(
                label = "Google Pay",
                icon = Res.drawable.gp,
                selected = state.selectedPaymentMethod == "Google Pay",
                onClick = { state.eventSink(CheckoutEvent.PaymentMethodSelected("Google Pay")) }
            )
        }

        item {
            WalletButton(
                label = "Apple Pay",
                icon = Res.drawable.applepay,
                selected = state.selectedPaymentMethod == "Apple Pay",
                onClick = { state.eventSink(CheckoutEvent.PaymentMethodSelected("Apple Pay")) }
            )
        }

        if (state.selectedPaymentMethod == "Credit Card") {
            item {
                val isCardComplete = state.cardDetails.cardNumber.length == 16 && state.cardDetails.nameOnCard.isNotBlank()
                
                if (isCardComplete) {
                    SummaryCardContainer(
                        title = "Saved Card",
                        onEditClicked = { state.eventSink(CheckoutEvent.ClearCardDetails) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(Res.drawable.mv),
                                contentDescription = null,
                                modifier = Modifier.height(44.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "**** **** **** ${state.cardDetails.cardNumber.takeLast(4)}",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = state.cardDetails.expiryDate,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CardField(
                            label = "Name on card",
                            value = state.cardDetails.nameOnCard,
                            onValueChange = { state.eventSink(CheckoutEvent.NameOnCardChanged(it)) },
                            placeholder = "Hikmet Atçeken",
                            imeAction = ImeAction.Next
                        )

                        CardField(
                            label = "Card number",
                            value = state.cardDetails.cardNumber,
                            onValueChange = { state.eventSink(CheckoutEvent.CardNumberChanged(it)) },
                            placeholder = "**** **** **** ****",
                            keyboardType = KeyboardType.Number,
                            visualTransformation = CardNumberVisualTransformation(),
                            imeAction = ImeAction.Next
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CardField(
                                label = "Expiry Date",
                                value = state.cardDetails.expiryDate,
                                onValueChange = { state.eventSink(CheckoutEvent.ExpiryDateChanged(it)) },
                                placeholder = "MM/YY",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                                imeAction = ImeAction.Next
                            )
                            CardField(
                                label = "Security code",
                                value = state.cardDetails.securityCode,
                                onValueChange = { state.eventSink(CheckoutEvent.SecurityCodeChanged(it)) },
                                placeholder = "CVC",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                                imeAction = ImeAction.Done
                            )
                        }

                        if (state.error != null) {
                            Text(
                                text = state.error,
                                color = Color.Red,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        BillingSameAsShippingRow(
                            checked = state.billingSameAsShipping,
                            onToggle = { state.eventSink(CheckoutEvent.BillingSameAsShippingToggled) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStep(state: CheckoutState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            OrderSummaryCard(
                items = state.cart.items,
                subtotal = state.cart.subtotal.toPriceString(),
                delivery = state.deliveryFee.toPriceString(),
                discount = if (state.cart.discountAmount > 0) state.cart.discountAmount.toPriceString() else null,
                total = (state.cart.total + state.deliveryFee).toPriceString()
            )
        }

        item {
            Column {
                Text(
                    text = "Please confirm and submit your order",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "By clicking submit order, you agree to Terms of Use and Privacy Policy",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SummaryCardContainer(title = "Payment", onEditClicked = { state.eventSink(CheckoutEvent.Back) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = state.customerName.ifBlank { "Guest Customer" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(Res.drawable.mv),
                            contentDescription = null,
                            modifier = Modifier.height(44.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (state.cardDetails.cardNumber.length >= 4) {
                                "**** **** **** ${state.cardDetails.cardNumber.takeLast(4)}"
                            } else {
                                "**** **** **** 0000"
                            },
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = state.cardDetails.expiryDate.ifBlank { "07/23" },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            SummaryCardContainer(title = "Shipping address", onEditClicked = { state.eventSink(CheckoutEvent.Back) }) {
                state.selectedAddress?.let { address ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Name", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = address.name,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("City", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = address.city,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Street", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = address.street,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCardSheet(state: CheckoutState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Add New Card", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        
        CardField(
            label = "Name on Card",
            value = state.cardDetails.nameOnCard,
            onValueChange = { state.eventSink(CheckoutEvent.NameOnCardChanged(it)) },
            placeholder = "Hikmet Atçeken",
            imeAction = ImeAction.Next
        )

        CardField(
            label = "Card Number",
            value = state.cardDetails.cardNumber,
            onValueChange = { state.eventSink(CheckoutEvent.CardNumberChanged(it)) },
            placeholder = "0000 0000 0000 0000",
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CardField(
                label = "Expiry Date",
                value = state.cardDetails.expiryDate,
                onValueChange = { state.eventSink(CheckoutEvent.ExpiryDateChanged(it)) },
                placeholder = "MM/YY",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                imeAction = ImeAction.Next
            )
            CardField(
                label = "CVC",
                value = state.cardDetails.securityCode,
                onValueChange = { state.eventSink(CheckoutEvent.SecurityCodeChanged(it)) },
                placeholder = "123",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                imeAction = ImeAction.Done
            )
        }

        CheckoutPrimaryButton(
            text = "Save Card",
            onClick = { state.eventSink(CheckoutEvent.ToggleAddCardSheet) },
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
