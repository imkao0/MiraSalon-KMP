package iz.mkao.mirasalon.feature.specialists.presentation.circuit

import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Specialist

data class SpecialistsState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val specialists: List<Specialist> = emptyList(),
    val filteredSpecialists: List<Specialist> = emptyList(),
    val error: String? = null,
    val eventSink: (SpecialistsEvent) -> Unit = {},
) : CircuitUiState
