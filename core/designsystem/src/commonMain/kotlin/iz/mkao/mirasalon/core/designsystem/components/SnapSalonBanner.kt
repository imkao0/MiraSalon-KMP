package iz.mkao.mirasalon.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.theme.*

@Composable
fun MiraPromoBanner(
    title: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageContent: @Composable () -> Unit = {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
        )
    },
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier =
            modifier
                .height(BannerHeight),
        shape = RoundedCornerShape(RadiusLarge),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = ElevationNone),
        border = BorderStroke(StrokeThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            imageContent()

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops =
                                    arrayOf(
                                        0.0f to Color.Transparent,
                                        0.7f to backgroundColor.copy(alpha = 0.8f),
                                        1.0f to backgroundColor,
                                    ),
                            ),
                        ),
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(SpacingIntermediate)
                        .align(Alignment.BottomStart),
            ) {
                Text(
                    text = title,
                    color = contentColor,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(SpacingDefault))

                MiraButton(
                    onClick = onButtonClick,
                    text = buttonText,
                    modifier = Modifier.widthIn(min = PromoBannerButtonMinWidth),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MiraPromoBannerPreview() {
    MiraSalonTheme {
        MiraPromoBanner(
            title = "Special Offer 50% Off",
            buttonText = "Book Now",
            onButtonClick = {},
            modifier = Modifier.padding(SpacingMedium),
        )
    }
}
