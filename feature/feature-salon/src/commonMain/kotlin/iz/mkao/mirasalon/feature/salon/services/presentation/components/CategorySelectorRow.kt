package iz.mkao.mirasalon.feature.salon.services.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.designsystem.components.categoryIconVector
import iz.mkao.mirasalon.core.domain.model.ServiceCategory

@Composable
fun CategorySelectorRow(
    categories: List<ServiceCategory>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allCategories = listOf(null) + categories.map { it.id }
    val categoryMap = categories.associateBy { it.id }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        allCategories.chunked(4).forEach { rowIds ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowIds.forEach { id ->
                    val category = categoryMap[id]
                    CategoryCard(
                        label = category?.name ?: "All",
                        isSelected = id == selectedCategoryId,
                        onClick = { onSelect(id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowIds.size < 4) {
                    repeat(4 - rowIds.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val targetBackgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surface
    }
    val targetLabelColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isHovered -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val backgroundColor by animateColorAsState(targetBackgroundColor)
    val labelColor by animateColorAsState(targetLabelColor)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
