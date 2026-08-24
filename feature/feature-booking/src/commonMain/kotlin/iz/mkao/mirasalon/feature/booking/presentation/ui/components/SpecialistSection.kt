package iz.mkao.mirasalon.feature.booking.presentation.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.*
import iz.mkao.mirasalon.feature.booking.domain.model.BookingSpecialist
import iz.mkao.mirasalon.feature.booking.presentation.circuit.BookingState

@Composable
fun SpecialistSection(
    state: BookingState,
    onSpecialistSelected: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Select Specialist",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoadingSpecialists -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(4) {
                        Column(
                            modifier = Modifier.width(72.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            ) {
                                ShimmerLoading()
                            }
                        }
                    }
                }
            }
            state.specialists.isEmpty() -> {
                Text(
                    text = "No specialists available for this service.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(state.specialists.distinctBy { it.id }, key = { it.id }) { specialist ->
                        SpecialistAvatar(
                            specialist = specialist,
                            isSelected = (specialist.id == state.selectedSpecialistId),
                            onClick = { onSpecialistSelected(specialist.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialistAvatar(
    specialist: BookingSpecialist,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MiraBorder,
                    shape = CircleShape
                )
                .padding(2.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(specialist.imageUrl)
                    .crossfade(enable = true)
                    .build(),
                contentDescription = specialist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Outlined.Person)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = specialist.name.substringBefore(" "),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
