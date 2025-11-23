package iz.mkao.mirasalon.feature.specialists.presentation.circuit

import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.Specialist

data class SpecialistDetailState(
    val isLoading: Boolean = true,
    val specialist: Specialist? = null,
    val isLoggedIn: Boolean = false,
    val showReviewSheet: Boolean = false,
    val onReviewSubmit: (suspend (Int, String) -> Result<Unit>)? = null,
    val error: String? = null,
    val eventSink: (SpecialistDetailEvent) -> Unit = {},
) : CircuitUiState
