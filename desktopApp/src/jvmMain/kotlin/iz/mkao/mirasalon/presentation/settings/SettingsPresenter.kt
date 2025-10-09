package iz.mkao.mirasalon.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.model.AdminSalon
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.outcome.toThrowable
import iz.mkao.mirasalon.core.domain.repository.AdminSalonRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SettingsPresenter(
    private val repository: AdminSalonRepository
) : Presenter<SettingsUiState> {

    @Composable
    override fun present(): SettingsUiState {
        var salon by remember { mutableStateOf(SettingsUiState().salon) }
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf(SettingsUiState().error) }
        val scope = rememberCoroutineScope()
        var loadJob by remember { mutableStateOf<Job?>(null) }

        fun loadData() {
            loadJob?.cancel()
            loadJob = scope.launch {
                isLoading = true
                error = null
                try {
                    when (val result = repository.getManagementInfo()) {
                        is Outcome.Success -> {
                            salon = result.data.firstOrNull()
                            isLoading = false
                        }
                        is Outcome.Error -> {
                            error = result.failure.toThrowable().message ?: "Failed to load salon"
                            isLoading = false
                        }
                        else -> isLoading = false
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    error = e.message ?: "An unexpected error occurred"
                    isLoading = false
                }
            }
        }

        LaunchedEffect(Unit) { loadData() }

        return SettingsUiState(salon = salon, isLoading = isLoading, error = error) { event ->
            when (event) {
                is SettingsEvent.UpdateSalon -> scope.launch {
                    isLoading = true
                    try {
                        when (repository.updateSalon(event.salon.id, event.salon)) {
                            is Outcome.Success -> loadData()
                            is Outcome.Error -> {
                                error = "Failed to update salon"
                                isLoading = false
                            }
                            else -> isLoading = false
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        error = e.message ?: "An unexpected error occurred"
                        isLoading = false
                    }
                }
                SettingsEvent.Refresh -> loadData()
            }
        }
    }
}
