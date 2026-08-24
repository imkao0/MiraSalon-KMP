package iz.mkao.mirasalon.feature.booking.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.util.DateUtils
import iz.mkao.mirasalon.core.common.util.toPriceString
import iz.mkao.mirasalon.core.designsystem.applepay
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.gp
import iz.mkao.mirasalon.core.designsystem.mastercard
import iz.mkao.mirasalon.core.designsystem.theme.BottomNavDividerThickness
import iz.mkao.mirasalon.core.designsystem.theme.ButtonHeight
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeMedium
import iz.mkao.mirasalon.core.designsystem.theme.MiraPrimaryDeep
import iz.mkao.mirasalon.core.designsystem.theme.RadiusExtraSmall
import iz.mkao.mirasalon.core.designsystem.theme.RadiusSmall
import iz.mkao.mirasalon.core.designsystem.theme.SpacingDefault
import iz.mkao.mirasalon.core.designsystem.theme.SpacingIntermediate
import iz.mkao.mirasalon.core.designsystem.theme.SpacingLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSection
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall
import iz.mkao.mirasalon.core.designsystem.theme.StrokeThin
import iz.mkao.mirasalon.core.designsystem.visa
import iz.mkao.mirasalon.core.domain.model.PaymentMethod
import iz.mkao.mirasalon.core.domain.model.PaymentMethodType
import iz.mkao.mirasalon.feature.booking.Res
import iz.mkao.mirasalon.feature.booking.presentation.circuit.AppointmentCheckoutEvent
import iz.mkao.mirasalon.feature.booking.presentation.circuit.AppointmentCheckoutState
import iz.mkao.mirasalon.feature.booking.rule_serenity_desc
import iz.mkao.mirasalon.feature.booking.rule_serenity_title
import iz.mkao.mirasalon.feature.booking.rule_smoking_desc
import iz.mkao.mirasalon.feature.booking.rule_smoking_title
import iz.mkao.mirasalon.feature.booking.rule_staff_desc
import iz.mkao.mirasalon.feature.booking.rule_staff_title
import iz.mkao.mirasalon.feature.booking.rules_subtitle
import iz.mkao.mirasalon.feature.booking.rules_title
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import iz.mkao.mirasalon.core.designsystem.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentCheckoutUi(
    state: AppointmentCheckoutState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MiraTopAppBar(
                title = "Review Booking",
                onBackClick = { state.eventSink(AppointmentCheckoutEvent.Back) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share")
                    }
                }
            )
        },
        bottomBar = {
            CheckoutBottomBar(state)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                ShimmerLoading()
            }
        } else {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val futureDate = today.plus(DatePeriod(days = 14))

            val dateRange = if (today.month == futureDate.month) {
                "${today.day}-${futureDate.day} ${today.month.name.lowercase().replaceFirstChar { it.uppercase() }}"
            } else {
                "${today.day} ${today.month.name.lowercase().take(3).replaceFirstChar { it.uppercase() }} - " +
                        "${futureDate.day} ${futureDate.month.name.lowercase().take(3).replaceFirstChar { it.uppercase() }}"
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(SpacingLarge)
            ) {
                AppointmentSummarySection(state)

                PaymentMethodsSection(state)

                PolicySection(
                    title = "Cancellation Policy",
                    content = "Cancellations for bookings on $dateRange must be made at least 48 hours in advance to receive a refund."
                )

                RulesSection()
            }
        }

        if (state.showAddPaymentSheet) {
            AddPaymentBottomSheet(
                onDismiss = { state.eventSink(AppointmentCheckoutEvent.DismissAddPaymentSheet) },
                onSave = { type, name, number, exp, cvc ->
                    state.eventSink(AppointmentCheckoutEvent.SavePaymentMethod(type, name, number, exp, cvc))
                }
            )
        }
    }
}

@Composable
private fun AppointmentSummarySection(state: AppointmentCheckoutState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(StrokeThin, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(RadiusSmall)
            )
            .padding(SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(SpacingMedium)
    ) {
        Text(
            text = "Appointment Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        state.services.forEach { service ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${service.durationMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (service.discountPercent > 0) {
                        Text(
                            text = service.price.toPriceString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Spacer(modifier = Modifier.width(SpacingSmall))
                    }
                    Text(
                        text = service.discountedPrice.toPriceString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = StrokeThin)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(IconSizeMedium),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(SpacingSmall))
                val timeLabel = DateUtils.formatDateSeparator(state.dateTime / 1000) + ", " + DateUtils.formatTime(state.dateTime / 1000)
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            val totalDuration = state.services.sumOf { it.durationMinutes }
            Text(
                text = "$totalDuration min total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaymentMethodsSection(state: AppointmentCheckoutState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(StrokeThin, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(RadiusSmall)
            )
            .padding(SpacingMedium)
    ) {
        state.paymentMethods.forEach { method ->
            PaymentMethodRow(
                method = method,
                isSelected = state.selectedPaymentMethodId == method.id,
                onClick = { state.eventSink(AppointmentCheckoutEvent.PaymentMethodSelected(method.id)) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = SpacingDefault),
                thickness = StrokeThin,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        OutlinedButton(
            onClick = { state.eventSink(AppointmentCheckoutEvent.AddPaymentMethod) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(RadiusSmall),
            border = BorderStroke(StrokeThin, MaterialTheme.colorScheme.outline)
        ) {
            Text("Add Payment Method", color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun PaymentMethodRow(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp, 32.dp)
                    .clip(RoundedCornerShape(RadiusExtraSmall))
                    .border(StrokeThin, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(RadiusExtraSmall))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (method.type) {
                    PaymentMethodType.VISA -> DesignRes.drawable.visa
                    PaymentMethodType.MASTER_CARD -> DesignRes.drawable.mastercard
                    PaymentMethodType.GOOGLE_PAY -> DesignRes.drawable.gp
                    PaymentMethodType.APPLE_PAY -> DesignRes.drawable.applepay
                    else -> null
                }
                
                if (icon != null) {
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp, 20.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        Icons.Outlined.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(SpacingDefault))
            Column {
                Text(
                    text = "${method.type.displayName} ****${method.last4Digits ?: "0000"}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Exp: ${method.expiryDate ?: "MM/YY"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MiraPrimaryDeep,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun PolicySection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(SpacingDefault)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(StrokeThin, MaterialTheme.colorScheme.outlineVariant),
                    RoundedCornerShape(RadiusSmall)
                )
                .padding(SpacingMedium)
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun RulesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(SpacingDefault)) {
        Text(
            text = stringResource(Res.string.rules_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(StrokeThin, MaterialTheme.colorScheme.outlineVariant),
                    RoundedCornerShape(RadiusSmall)
                )
                .padding(SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(SpacingSmall)
        ) {
            Text(
                text = stringResource(Res.string.rules_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            RuleItem(
                stringResource(Res.string.rule_smoking_title),
                stringResource(Res.string.rule_smoking_desc)
            )
            RuleItem(
                stringResource(Res.string.rule_serenity_title),
                stringResource(Res.string.rule_serenity_desc)
            )
            RuleItem(
                stringResource(Res.string.rule_staff_title),
                stringResource(Res.string.rule_staff_desc)
            )
        }
    }
}

@Composable
private fun RuleItem(title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(SpacingSmall)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(SpacingDefault))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CheckoutBottomBar(state: AppointmentCheckoutState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(BottomNavDividerThickness, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.discountedAmount < state.totalAmount) {
                            Text(
                                text = state.totalAmount.toPriceString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Spacer(modifier = Modifier.width(SpacingSmall))
                        }
                        Text(
                            text = state.discountedAmount.toPriceString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " /Session",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Includes taxes and other fees.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    Napier.d("AppointmentCheckoutScreen: Continue button clicked")
                    state.eventSink(AppointmentCheckoutEvent.Continue)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonHeight),
                enabled = !state.isBooking,
                shape = RoundedCornerShape(RadiusSmall),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (state.isBooking) {
                    ShimmerLoading(
                        modifier = Modifier.size(IconSizeMedium)
                    )
                } else {
                    Text(
                        "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentBottomSheet(
    onDismiss: () -> Unit,
    onSave: (type: String, name: String, number: String, exp: String, cvc: String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusSmall, topEnd = RadiusSmall),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = SpacingDefault)
                    .size(36.dp, 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        var cardType by remember { mutableStateOf("Visa") }
        var nameOnCard by remember { mutableStateOf("") }
        var cardNumber by remember { mutableStateOf("") }
        var expiry by remember { mutableStateOf("") }
        var cvc by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingLarge)
                .padding(bottom = SpacingSection),
            verticalArrangement = Arrangement.spacedBy(SpacingIntermediate)
        ) {
            Text(
                "Add New Card",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(SpacingSmall)) {
                listOf("Visa", "Master Card").forEach { type ->
                    FilterChip(
                        selected = cardType == type,
                        onClick = { cardType = type },
                        label = { Text(type) },
                        shape = RoundedCornerShape(RadiusSmall)
                    )
                }
            }

            LocalCardField(
                label = "Name on card",
                value = nameOnCard,
                onValueChange = { nameOnCard = it },
                placeholder = "John Doe",
                imeAction = ImeAction.Next
            )

            LocalCardField(
                label = "Card number",
                value = cardNumber,
                onValueChange = { if (it.length <= 16) cardNumber = it.filter { it.isDigit() } },
                placeholder = "0000 0000 0000 0000",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                visualTransformation = LocalCardNumberVisualTransformation()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(SpacingMedium)) {
                LocalCardField(
                    label = "Expiry Date",
                    value = expiry,
                    onValueChange = { if (it.length <= 5) expiry = it },
                    placeholder = "MM/YY",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.weight(1f)
                )
                LocalCardField(
                    label = "Security code",
                    value = cvc,
                    onValueChange = { if (it.length <= 4) cvc = it.filter { it.isDigit() } },
                    placeholder = "CVC",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = { onSave(cardType, nameOnCard, cardNumber, expiry, cvc) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonHeight),
                shape = RoundedCornerShape(RadiusSmall),
                enabled = cardNumber.length >= 12 && expiry.length >= 4 && nameOnCard.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save Card", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun LocalCardField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(SpacingSmall))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(RadiusSmall),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

private class LocalCardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val trimmed = if (text.text.length >= 16) text.text.substring(0..15) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != 15) out += " "
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset + 1
                if (offset <= 11) return offset + 2
                if (offset <= 16) return offset + 3
                return 19
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                if (offset <= 19) return offset - 3
                return 16
            }
        }

        return androidx.compose.ui.text.input.TransformedText(AnnotatedString(out), offsetMapping)
    }
}
