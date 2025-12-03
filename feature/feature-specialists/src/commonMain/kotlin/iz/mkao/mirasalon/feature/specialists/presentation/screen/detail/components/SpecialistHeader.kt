package iz.mkao.mirasalon.feature.specialists.presentation.screen.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.common.util.formatRating
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.designsystem.theme.*
import iz.mkao.mirasalon.core.network.config.ApiEndpoints

@Composable
fun SpecialistHeader(specialist: Specialist) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(IconSizeExtraLarge + SpacingLarge) // ~88dp
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = ApiEndpoints.resolveImageUrl(specialist.imageUrl),
                contentDescription = specialist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.width(SpacingMedium))

        Column {
            Text(specialist.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(specialist.role, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(SpacingSmall))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (specialist.isOnline) Color(0xFF4CAF50) else Color.Gray)
                )
                Spacer(modifier = Modifier.width(SpacingSmall))
                Text(
                    text = if (specialist.isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (specialist.isOnline) Color(0xFF4CAF50) else Color.Gray
                )
            }
        }
    }
}
