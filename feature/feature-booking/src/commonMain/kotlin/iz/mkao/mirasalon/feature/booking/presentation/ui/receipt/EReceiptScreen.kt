package iz.mkao.mirasalon.feature.booking.presentation.ui.receipt

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import iz.mkao.mirasalon.core.common.util.toPriceString
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.RadiusSmall
import iz.mkao.mirasalon.feature.booking.presentation.circuit.EReceiptEvent
import iz.mkao.mirasalon.feature.booking.presentation.circuit.EReceiptState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EReceiptUi(
    state: EReceiptState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is EReceiptState.Loading -> {
            Scaffold(
                modifier = modifier,
                topBar = {
                    MiraTopAppBar(title = "E-Receipt", onBackClick = { })
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    ShimmerLoading()
                }
            }
        }
        is EReceiptState.Error -> {
            Scaffold(
                modifier = modifier,
                topBar = {
                    MiraTopAppBar(
                        title = "E-Receipt",
                        onBackClick = { state.eventSink(EReceiptEvent.BackClicked) }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        is EReceiptState.Success -> {
            val receipt = state.booking
            Scaffold(
                modifier = modifier,
                topBar = {
                    MiraTopAppBar(
                        title = "E-Receipt",
                        onBackClick = { state.eventSink(EReceiptEvent.CloseClicked) },
                        navigationIcon = {
                            IconButton(onClick = { state.eventSink(EReceiptEvent.CloseClicked) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Button(
                            onClick = { state.eventSink(EReceiptEvent.ViewBookingsClicked) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(RadiusSmall)
                        ) {
                            Text("Done", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Image(
                        painter = rememberQrCodePainter(
                            data = receipt.qrPayload.ifBlank { receipt.id.take(6) }
                        ) {
                            colors {
                                dark = QrBrush.solid(Color.Black)
                            }
                            background {
                                fill = SolidColor(Color.White)
                            }
                        },
                        contentDescription = "QR code",
                        modifier = Modifier.size(140.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Please scan your QR code at the\nsalon's scanner machine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        ReceiptGridRow(
                            leftLabel = "Booking ID",
                            leftValue = receipt.id.take(6),
                            rightLabel = "Customer",
                            rightValue = receipt.customerName
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        ReceiptGridRow(
                            leftLabel = "Phone",
                            leftValue = receipt.customerPhone.ifBlank { "-" },
                            rightLabel = "Salon",
                            rightValue = receipt.salonName.ifBlank { "Mira Salon" }
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        val bookingDate = Instant.fromEpochMilliseconds(receipt.dateTime)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                        val formattedDate = "${bookingDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${bookingDate.dayOfMonth}, ${bookingDate.year}"
                        
                        ReceiptGridRow(
                            leftLabel = "Booking Date",
                            leftValue = formattedDate,
                            rightLabel = "Booking Time",
                            rightValue = receipt.timeSlotLabel.ifBlank { 
                                "${bookingDate.hour.toString().padStart(2, '0')}:${bookingDate.minute.toString().padStart(2, '0')}"
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        if (receipt.createdAt > 0) {
                            val createdDate = Instant.fromEpochMilliseconds(receipt.createdAt)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                            val formattedCreatedDate = "${createdDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${createdDate.dayOfMonth}, ${createdDate.year}"
                            
                            ReceiptField(
                                label = "Booking Placed",
                                value = formattedCreatedDate
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        receipt.services.forEachIndexed { index, service ->
                            if (index > 0) Spacer(modifier = Modifier.height(24.dp))
                            ReceiptGridRow(
                                leftLabel = if (index == 0) "Service" else null,
                                leftValue = service.name,
                                rightLabel = if (index == 0) "Stylist" else null,
                                rightValue = receipt.specialistName
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))

                        ReceiptTotalRow(label = "Sub Total", value = receipt.subtotalAmount.toPriceString())
                        Spacer(modifier = Modifier.height(12.dp))
                        if (receipt.discountAmount > 0) {
                            ReceiptTotalRow(label = "Discount", value = "-${receipt.discountAmount.toPriceString()}")
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        if (receipt.taxAmount > 0) {
                            ReceiptTotalRow(label = "Sales Tax (${receipt.taxRatePercent.toInt()}%)", value = receipt.taxAmount.toPriceString())
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        ReceiptTotalRow(label = "Total Amount", value = receipt.totalAmount.toPriceString(), emphasized = true)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ReceiptGridRow(
    leftLabel: String?,
    leftValue: String,
    rightLabel: String?,
    rightValue: String,
    onLeftValueClick: (() -> Unit)? = null,
    onRightValueClick: (() -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        ReceiptField(
            label = leftLabel,
            value = leftValue,
            modifier = Modifier.weight(1f),
            onClick = onLeftValueClick
        )
        Spacer(modifier = Modifier.width(16.dp))
        ReceiptField(
            label = rightLabel,
            value = rightValue,
            modifier = Modifier.weight(1f),
            onClick = onRightValueClick
        )
    }
}

@Composable
private fun ReceiptField(
    label: String?,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReceiptTotalRow(label: String, value: String, emphasized: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
