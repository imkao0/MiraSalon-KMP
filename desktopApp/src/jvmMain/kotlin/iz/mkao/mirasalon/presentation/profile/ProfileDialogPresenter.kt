package iz.mkao.mirasalon.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.common.result.NetworkResult
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.UploadRepository
import iz.mkao.mirasalon.data.local.TokenManager
import iz.mkao.mirasalon.data.remote.AuthClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ProfileDialogPresenter(
    private val tokenManager: TokenManager,
    private val authClient: AuthClient,
    private val uploadRepository: UploadRepository,
    private val onDismiss: () -> Unit
) : Presenter<ProfileDialogState> {

    @Composable
    override fun present(): ProfileDialogState {
        val session by tokenManager.session.collectAsState()
        val currentFirstName = session.firstName
        val currentLastName = session.lastName
        val currentPhone = session.phone
        val currentAddress = session.address
        val currentGender = session.gender
        val currentAvatar = session.avatarUrl

        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var gender by remember { mutableStateOf("") }
        var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
        var selectedImageName by remember { mutableStateOf<String?>(null) }

        var isLoading by remember { mutableStateOf(false) }
        var uploadProgress by remember { mutableFloatStateOf(0f) }
        var message by remember { mutableStateOf<String?>(null) }


        LaunchedEffect(currentFirstName, currentLastName, currentPhone, currentAddress, currentGender) {
            if (firstName.isEmpty()) firstName = currentFirstName ?: ""
            if (lastName.isEmpty()) lastName = currentLastName ?: ""
            if (phone.isEmpty()) phone = currentPhone ?: ""
            if (address.isEmpty()) address = currentAddress ?: ""
            if (gender.isEmpty()) gender = currentGender ?: ""
        }


        LaunchedEffect(Unit) {
            when (val result = authClient.getProfile()) {
                is NetworkResult.Success -> {
                    tokenManager.updateProfile(
                        firstName = result.data.firstName,
                        lastName = result.data.lastName,
                        name = result.data.name,
                        phone = result.data.phone,
                        address = result.data.address,
                        gender = result.data.gender,
                        avatar = result.data.avatarUrl
                    )
                }
                else -> {

                }
            }
        }

        val scope = rememberCoroutineScope()

        return ProfileDialogState(
            firstName = firstName,
            lastName = lastName,
            phone = phone,
            address = address,
            gender = gender,
            avatarUrl = currentAvatar,
            selectedImageBytes = selectedImageBytes,
            selectedImageName = selectedImageName,
            isLoading = isLoading,
            uploadProgress = uploadProgress,
            message = message,
            eventSink = { event ->
                when (event) {
                    is ProfileDialogEvent.FirstNameChanged -> firstName = event.value
                    is ProfileDialogEvent.LastNameChanged -> lastName = event.value
                    is ProfileDialogEvent.PhoneChanged -> phone = event.value
                    is ProfileDialogEvent.AddressChanged -> address = event.value
                    is ProfileDialogEvent.GenderChanged -> gender = event.value
                    is ProfileDialogEvent.ImageSelected -> {
                        selectedImageBytes = event.bytes
                        selectedImageName = event.name
                        if (event.name != null) {
                            message = "Image selected: ${event.name}"
                        }
                    }
                    ProfileDialogEvent.SaveClicked -> {
                        scope.launch {
                            isLoading = true
                            message = "Updating profile..."
                            uploadProgress = 0.1f

                            var finalAvatarUrl = currentAvatar

                            if (selectedImageBytes != null && selectedImageName != null) {
                                uploadProgress = 0.3f
                                val uploadResult = uploadRepository.uploadImage(selectedImageBytes!!, selectedImageName!!)
                                when (uploadResult) {
                                    is Outcome.Success -> {
                                        finalAvatarUrl = uploadResult.data
                                        uploadProgress = 0.8f
                                    }
                                    is Outcome.Error -> {
                                        message = "Failed to upload image: ${uploadResult.failure}. Trying to update name only..."
                                    }
                                    else -> {}
                                }
                            }

                            val result = authClient.updateProfile(
                                firstName = firstName,
                                lastName = lastName,
                                phone = phone,
                                address = address,
                                gender = gender,
                                avatarUrl = finalAvatarUrl
                            )
                            when (result) {
                                is NetworkResult.Success -> {
                                    uploadProgress = 1.0f
                                    tokenManager.updateProfile(
                                        firstName = firstName,
                                        lastName = lastName,
                                        name = "$firstName $lastName".trim(),
                                        phone = phone,
                                        address = address,
                                        gender = gender,
                                        avatar = finalAvatarUrl
                                    )
                                    message = "Profile updated successfully!"
                                    delay(800.milliseconds)
                                    onDismiss()
                                }
                                is NetworkResult.Error -> {
                                    message = "Failed to update profile info: ${result.error}"
                                }
                                else -> {}
                            }
                            isLoading = false
                        }
                    }
                    ProfileDialogEvent.Dismiss -> onDismiss()
                }
            }
        )
    }
}
