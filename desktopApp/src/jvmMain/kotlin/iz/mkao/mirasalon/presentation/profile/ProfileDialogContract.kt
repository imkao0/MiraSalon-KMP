package iz.mkao.mirasalon.presentation.profile

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

sealed interface ProfileDialogEvent : CircuitUiEvent {
    data class FirstNameChanged(val value: String) : ProfileDialogEvent
    data class LastNameChanged(val value: String) : ProfileDialogEvent
    data class PhoneChanged(val value: String) : ProfileDialogEvent
    data class AddressChanged(val value: String) : ProfileDialogEvent
    data class GenderChanged(val value: String) : ProfileDialogEvent
    data class ImageSelected(val bytes: ByteArray?, val name: String?) : ProfileDialogEvent
    data object SaveClicked : ProfileDialogEvent
    data object Dismiss : ProfileDialogEvent
}

data class ProfileDialogState(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val address: String = "",
    val gender: String = "",
    val avatarUrl: String? = null,
    val selectedImageBytes: ByteArray? = null,
    val selectedImageName: String? = null,
    val isLoading: Boolean = false,
    val uploadProgress: Float = 0f,
    val message: String? = null,
    val eventSink: (ProfileDialogEvent) -> Unit = {}
) : CircuitUiState
