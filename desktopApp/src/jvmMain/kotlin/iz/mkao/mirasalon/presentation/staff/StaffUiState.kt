package iz.mkao.mirasalon.presentation.staff

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.core.domain.model.AdminSpecialist
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistShift
import iz.mkao.mirasalon.core.domain.model.AdminSpecialistStats
import iz.mkao.mirasalon.core.domain.model.Service

data class StaffUiState(
    val staff: List<AdminSpecialist> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedStaffStats: AdminSpecialistStats? = null,
    val isStatsLoading: Boolean = false,
    val uploadProgress: Float = 0f,
    val allServices: List<Service> = emptyList(),
    val error: String? = null,
    val eventSink: (StaffEvent) -> Unit = {}
) : CircuitUiState

sealed class UploadState {
    data object Idle : UploadState()
    data class Progress(val progress: Float) : UploadState()
    data class Success(val url: String) : UploadState()
    data class Error(val message: String) : UploadState()
}

/** Circuit UI events for the staff admin screen. */
sealed interface StaffEvent : CircuitUiEvent {
    data class Search(val query: String) : StaffEvent
    data class CreateStaff(val specialist: AdminSpecialist) : StaffEvent
    data class UpdateStaff(val specialist: AdminSpecialist) : StaffEvent
    data class DeleteStaff(val id: String) : StaffEvent
    data class LoadStats(val id: String) : StaffEvent
    data class UpdateShifts(val id: String, val shifts: List<AdminSpecialistShift>) : StaffEvent
    data class SetAvailability(val id: String, val isAvailable: Boolean) : StaffEvent
    data class ToggleActive(val id: String, val isActive: Boolean) : StaffEvent
    data class UploadImage(val bytes: ByteArray, val name: String, val onResult: (String?) -> Unit) : StaffEvent
    data object Refresh : StaffEvent
}
