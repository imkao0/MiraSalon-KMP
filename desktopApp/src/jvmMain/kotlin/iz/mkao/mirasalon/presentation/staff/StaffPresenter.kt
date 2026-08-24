package iz.mkao.mirasalon.presentation.staff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.common.result.NetworkResult
import iz.mkao.mirasalon.core.domain.model.ServiceFilter
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.toNetworkResult
import iz.mkao.mirasalon.core.domain.repository.AdminSpecialistRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceRepository
import iz.mkao.mirasalon.core.domain.repository.UploadRepository
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StaffPresenter(
    private val repository: AdminSpecialistRepository,
    private val uploadRepository: UploadRepository,
    private val serviceRepository: ServiceRepository,
    private val realtimeGateway: RealtimeGateway
) : Presenter<StaffUiState> {

    @Composable
    override fun present(): StaffUiState {
        var searchQuery by remember {
            mutableStateOf("")
        }
        var selectedStaffIdForStats by remember {
            mutableStateOf<String?>(null)
        }
        var uploadProgress by remember {
            mutableStateOf(0f)
        }
        var refreshTrigger by remember {
            mutableStateOf(0L)
        }

        val scope = rememberCoroutineScope()

        val uiState by produceState(
            initialValue = StaffUiState(
                searchQuery = searchQuery,
                isLoading = true
            ),
            key1 = selectedStaffIdForStats,
            key2 = searchQuery,
            key3 = refreshTrigger
        ) {
            value = value.copy(isLoading = true, error = null)

            try {
                coroutineScope {
                    val staffDeferred = async { repository.getAll(query = searchQuery.ifBlank { null }).toNetworkResult() }
                    val servicesDeferred = async { serviceRepository.getServices(ServiceFilter()).toNetworkResult() }
                    val statsDeferred = if (selectedStaffIdForStats != null) {
                        async { repository.getStats(selectedStaffIdForStats!!).toNetworkResult() }
                    } else {
                        null
                    }

                    val staffResult = staffDeferred.await()
                    val servicesResult = servicesDeferred.await()
                    val statsResult = statsDeferred?.await()

                    var newStaff = value.staff
                    var newServices = value.allServices
                    var newStats = value.selectedStaffStats
                    val errorMessage: String? = null

                    if (staffResult is NetworkResult.Success) {
                        newStaff = staffResult.data
                        Napier.d { "[StaffPresenter] Loaded ${newStaff.size} staff members" }
                        newStaff.forEach { s ->
                            Napier.v { "[StaffPresenter] Specialist: ${s.name}, Image: ${s.imageUrl}" }
                        }
                    }

                    if (servicesResult is NetworkResult.Success) {
                        newServices = servicesResult.data
                    }

                    if (statsResult is NetworkResult.Success) {
                        newStats = statsResult.data
                    } else if (selectedStaffIdForStats == null) {
                        newStats = null
                    }

                    value = value.copy(
                        staff = newStaff,
                        allServices = newServices,
                        selectedStaffStats = newStats,
                        searchQuery = searchQuery,
                        error = errorMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                value = value.copy(
                    error = "System error: ${e.message}",
                    isLoading = false
                )
            }
        }

        LaunchedEffect(Unit) {
            realtimeGateway.events.collectLatest { event ->
                when (event) {
                    is DomainEvent.SpecialistStatusChanged,
                    is DomainEvent.ReviewSubmitted -> {
                        refreshTrigger = System.currentTimeMillis()
                    }
                    else -> Unit
                }
            }
        }

        return uiState.copy(
            uploadProgress = uploadProgress,
            eventSink = { event ->
                when (event) {
                    is StaffEvent.Search -> {
                        searchQuery = event.query
                    }

                    is StaffEvent.CreateStaff -> {
                        scope.launch {
                            try {
                                when (repository.create(event.specialist).toNetworkResult()) {
                                    is NetworkResult.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }

                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is StaffEvent.UpdateStaff -> {
                        scope.launch {
                            try {
                                when (repository.update(event.specialist).toNetworkResult()) {
                                    is NetworkResult.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }

                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is StaffEvent.DeleteStaff -> {
                        scope.launch {
                            try {
                                when (repository.delete(event.id).toNetworkResult()) {
                                    is NetworkResult.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }

                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is StaffEvent.LoadStats -> {
                        selectedStaffIdForStats = event.id
                    }

                    is StaffEvent.UpdateShifts -> {
                        scope.launch {
                            repository.updateShifts(event.id, event.shifts).toNetworkResult()
                            refreshTrigger = System.currentTimeMillis()
                        }
                    }

                    is StaffEvent.SetAvailability -> {
                        scope.launch {
                            when (repository.updateAvailability(event.id, event.isAvailable).toNetworkResult()) {
                                is NetworkResult.Success -> refreshTrigger = System.currentTimeMillis()
                                else -> Unit
                            }
                        }
                    }

                    is StaffEvent.ToggleActive -> {
                        scope.launch {
                            repository.updateActiveStatus(event.id, event.isActive).toNetworkResult()
                            refreshTrigger = System.currentTimeMillis()
                        }
                    }

                    is StaffEvent.UploadImage -> {
                        scope.launch {
                            uploadProgress = 0.1f
                            val result = uploadRepository.uploadImage(event.bytes, event.name, "image/jpeg").toNetworkResult()
                            Napier.d { "[StaffPresenter] Image upload result: $result" }
                            val url = (result as? NetworkResult.Success)?.data
                            uploadProgress = if (url != null) 1.0f else 0f
                            event.onResult(url)
                        }
                    }

                    StaffEvent.Refresh -> {
                        refreshTrigger = System.currentTimeMillis()
                    }
                }
            }
        )
    }
}
