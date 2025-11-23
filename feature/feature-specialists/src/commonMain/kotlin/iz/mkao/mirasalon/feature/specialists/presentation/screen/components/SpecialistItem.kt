package iz.mkao.mirasalon.feature.specialists.presentation.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeSmall
import iz.mkao.mirasalon.core.designsystem.theme.RadiusMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingDefault
import iz.mkao.mirasalon.core.designsystem.theme.SpacingIntermediate
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall
import iz.mkao.mirasalon.core.designsystem.theme.SpacingTiny
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.network.config.ApiEndpoints

/**
 * Specialist item card matching the requested profile style:
 * portrait photo, bottom gradient scrim, name + verified badge,
 * role/bio, stats, and a "Book" action button.
 */
@Composable
fun SpecialistItem(
    specialist: Specialist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(RadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {

        val imageUrl = ApiEndpoints.resolveImageUrl(specialist.imageUrl)
        
        AsyncImage(
            model = imageUrl,
            contentDescription = specialist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Rating badge at top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(SpacingMedium)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD700)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = specialist.rating.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.5f to MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            1.0f to MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(SpacingIntermediate),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = specialist.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (specialist.isVerified) {
                        Gap(SpacingSmall)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD700)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Verified",
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }

                Gap(SpacingTiny)
                Text(
                    text = specialist.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Gap(SpacingDefault)
                Row(horizontalArrangement = Arrangement.spacedBy(SpacingDefault)) {
                    StatItem(icon = Icons.Outlined.Person, value = specialist.customersCount)
                    StatItem(icon = Icons.Outlined.Check, value = specialist.yearsOfExperience)
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSizeSmall),
        )
        Gap(SpacingTiny)
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Gap(size: Dp) {
    Spacer(modifier = Modifier.size(size))
}
