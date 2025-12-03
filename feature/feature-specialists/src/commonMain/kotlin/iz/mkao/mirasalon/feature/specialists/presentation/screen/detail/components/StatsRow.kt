package iz.mkao.mirasalon.feature.specialists.presentation.screen.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.common.util.formatRating
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeMedium
import iz.mkao.mirasalon.core.domain.model.Specialist

@Composable
fun StatsRow(
    specialist: Specialist,
    onRatingClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatItem(icon = Icons.Outlined.Group, value = specialist.customersCount.toString(), label = "Customers")
        StatItem(icon = Icons.Outlined.Work, value = "${specialist.yearsOfExperience}+", label = "Years Exp")
        StatItem(
            icon = Icons.Outlined.Star,
            value = specialist.rating.formatRating(),
            label = "Rating",
            iconColor = Color(0xFFFFD700),
            modifier = Modifier.clickable(onClick = onRatingClick)
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(IconSizeMedium), tint = iconColor)
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
