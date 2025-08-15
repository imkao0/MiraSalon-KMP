package iz.mkao.mirasalon.feature.products.presentation.circuit.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.common.util.toPriceString
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.feature.products.presentation.circuit.theme.ExploreCategoriesCardShape
import iz.mkao.mirasalon.feature.products.presentation.circuit.theme.ExploreCategoriesColors
import iz.mkao.mirasalon.feature.products.presentation.circuit.theme.ExploreCategoriesOverlayButtonShape
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ExploreProductItem(
    product: Product,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSale: Boolean = false
) {
    val colors = ExploreCategoriesColors
    var isAdded by remember { mutableStateOf(false) }

    LaunchedEffect(isAdded) {
        if (isAdded) {
            delay(1000.milliseconds)
            isAdded = false
        }
    }

    val buttonBackground by animateColorAsState(
        targetValue = colors.overlayButtonBackground,
        label = "buttonBackground"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .clip(ExploreCategoriesCardShape)
                .background(colors.imagePlaceholder),
        ) {
            val resolvedImageUrl = ApiEndpoints.resolveImageUrl(product.imageUrl)
            AsyncImage(
                model = resolvedImageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            if (isSale || product.discountPercent > 0) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = "SALE",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(ExploreCategoriesOverlayButtonShape)
                    .background(buttonBackground)
                    .clickable {
                        isAdded = true
                        onAddClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isAdded,
                    label = "iconAnimation"
                ) { added ->
                    Icon(
                        imageVector = if (added) Icons.Outlined.Check else Icons.Outlined.Add,
                        contentDescription = if (added) "Added" else "Add to cart",
                        tint = if (added) MaterialTheme.colorScheme.primary else colors.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Text(
            text = product.name,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )

        Text(
            text = product.discountedPrice.toPriceString(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
