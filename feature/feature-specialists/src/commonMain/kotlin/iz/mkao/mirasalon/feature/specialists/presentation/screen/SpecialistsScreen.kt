package iz.mkao.mirasalon.feature.specialists.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.designsystem.components.MiraEmptyState
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.navigation.SpecialistRoute
import iz.mkao.mirasalon.feature.specialists.presentation.circuit.SpecialistDetailState
import iz.mkao.mirasalon.feature.specialists.presentation.circuit.SpecialistsEvent
import iz.mkao.mirasalon.feature.specialists.presentation.circuit.SpecialistsState
import iz.mkao.mirasalon.feature.specialists.presentation.screen.components.SpecialistItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialistsUi(
    state: SpecialistsState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MiraTopAppBar(
                title = "Salon Specialists",
                onBackClick = { state.eventSink(SpecialistsEvent.Back) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                state.isLoading -> {
                    ShimmerLoading(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null -> {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    if (state.specialists.isEmpty()) {
                        MiraEmptyState(
                            message = "No specialists found",
                            description = "We couldn't find any specialists at the moment. Please check back later or try a different search.",
                            icon = Icons.Outlined.Person
                        )
                    } else {
                        SpecialistsGrid(
                            specialists = state.specialists,
                            onSpecialistClick = { state.eventSink(SpecialistsEvent.SpecialistClicked(it)) }
                        )
                    }
                }
            }
        }
    }
}

class SpecialistsManualUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is SpecialistRoute.Specialists -> ui<SpecialistsState> { state, modifier -> SpecialistsUi(state, modifier) }
            is SpecialistRoute.SpecialistDetail -> ui<SpecialistDetailState> { state, modifier -> SpecialistDetailUi(state, modifier) }
            else -> null
        }
    }
}

@Composable
private fun SpecialistsGrid(
    specialists: List<Specialist>,
    onSpecialistClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(specialists.distinctBy { it.id }, key = { it.id }) { specialist ->
            SpecialistItem(
                specialist = specialist,
                onClick = { onSpecialistClick(specialist.id) },
            )
        }
    }
}
