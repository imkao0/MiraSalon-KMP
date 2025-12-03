package iz.mkao.mirasalon.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerLoading(
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    highlightColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val transitionProgress by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor,
        ),
        start = Offset(transitionProgress, transitionProgress),
        end = Offset(transitionProgress + 300f, transitionProgress + 300f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    )
}

/**
 * A rounded block that shimmers. Used for skeleton UI.
 */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    highlightColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(baseColor)
    ) {
        ShimmerLoading(
            baseColor = baseColor,
            highlightColor = highlightColor
        )
    }
}
