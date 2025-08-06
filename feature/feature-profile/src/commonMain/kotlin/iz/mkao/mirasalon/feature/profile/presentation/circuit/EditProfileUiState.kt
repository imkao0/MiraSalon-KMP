package iz.mkao.mirasalon.feature.profile.presentation.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import iz.mkao.mirasalon.feature.profile.domain.model.Gender

data class EditProfileState(
    val isLoading: Boolean,
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val avatarUrl: String?,
    val localImageBytes: ByteArray?,
    val gender: Gender?,
    val dateOfBirth: String? = null,
    val allergyInput: String = "",
    val allergies: List<String> = emptyList(),
    val isSaving: Boolean,
    val isUploadingImage: Boolean,
    val saveError: String?,
    val saved: Boolean = false,
    val eventSink: (EditProfileEvent) -> Unit
) : CircuitUiState

sealed interface EditProfileEvent : CircuitUiEvent {
    data class FullNameChanged(val value: String) : EditProfileEvent
    data class PhoneChanged(val value: String) : EditProfileEvent
    data class GenderSelected(val value: Gender?) : EditProfileEvent
    data class DateOfBirthChanged(val value: String?) : EditProfileEvent
    data class AllergyInputChanged(val value: String) : EditProfileEvent
    data object AddAllergy : EditProfileEvent
    data class RemoveAllergy(val allergy: String) : EditProfileEvent
    data class ImageSelected(val bytes: ByteArray) : EditProfileEvent
    data object Save : EditProfileEvent
    data object Back : EditProfileEvent
}
