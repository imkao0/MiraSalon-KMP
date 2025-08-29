package iz.mkao.mirasalon.feature.specialists.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ReviewBottomSheet
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.feature.specialists.presentation.circuit.SpecialistDetailEvent
import iz.mkao.mirasalon.feature.specialists.presentation.circuit.SpecialistDetailState
import iz.mkao.mirasalon.feature.specialists.presentation.screen.detail.components.ReviewRow
import iz.mkao.mirasalon.feature.specialists.presentation.screen.detail.components.ServiceRow
import iz.mkao.mirasalon.feature.specialists.presentation.screen.detail.components.SpecialistHeader
import iz.mkao.mirasalon.feature.specialists.presentation.screen.detail.components.StatsRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialistDetailUi(
    state: SpecialistDetailState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MiraTopAppBar(title = "", onBackClick = {
                state.eventSink(SpecialistDetailEvent.Back)
            })
        },
        bottomBar = {},
        floatingActionButton = {
            state.specialist?.let { specialist ->
                FloatingActionButton(onClick = { state.eventSink(SpecialistDetailEvent.ChatClicked(specialist)) }) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message")
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> {
                    ShimmerLoading(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null -> {
                    Text(state.error, modifier = Modifier.align(Alignment.Center))
                }

                state.specialist != null -> {
                    SpecialistDetailContent(
                        specialist = state.specialist,
                        onBookServiceClick = { state.eventSink(SpecialistDetailEvent.BookServiceClicked(it)) },
                        onWriteReviewClick = { state.eventSink(SpecialistDetailEvent.WriteReviewClicked) }
                    )
                }
            }

            if (state.showReviewSheet && state.onReviewSubmit != null) {
                ReviewBottomSheet(
                    onDismiss = { state.eventSink(SpecialistDetailEvent.DismissReviewSheet) },
                    onReviewSubmit = state.onReviewSubmit
                )
            }
        }
    }
}


@Composable
private fun SpecialistDetailContent(
    specialist: Specialist,
    onBookServiceClick: (String) -> Unit,
    onWriteReviewClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        item {
            SpecialistHeader(specialist)
            StatsRow(
                specialist = specialist,
                onRatingClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(index = if (specialist.services.isEmpty()) 4 else 14)
                    }
                }
            )
            if (specialist.bio.isNotBlank()) {
                SectionLabel(text = "About")
                Text(
                    text = specialist.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            SectionLabel(text = "Services")
        }

        if (specialist.services.isEmpty()) {
            item { EmptySectionText("No services listed yet") }
        } else {
            items(specialist.services) { service ->
                ServiceRow(service = service, onBookClick = { onBookServiceClick(service.id) })
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel(text = "Reviews (${specialist.reviews.size})")
                TextButton(onClick = onWriteReviewClick) {
                    Text("Write a Review")
                }
            }
        }

        if (specialist.reviews.isEmpty()) {
            item { EmptySectionText("No reviews yet") }
        } else {
            items(specialist.reviews) { review ->
                ReviewRow(review)
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun EmptySectionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
