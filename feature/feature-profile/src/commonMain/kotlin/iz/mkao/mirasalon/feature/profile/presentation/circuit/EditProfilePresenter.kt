package iz.mkao.mirasalon.feature.profile.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.feature.profile.domain.model.Gender
import iz.mkao.mirasalon.feature.profile.domain.model.ProfileUpdate
import iz.mkao.mirasalon.feature.profile.domain.repository.ProfileRepository
import iz.mkao.mirasalon.feature.profile.presentation.circuit.EditProfileEvent
import iz.mkao.mirasalon.feature.profile.presentation.circuit.EditProfileState
import kotlinx.coroutines.launch

class EditProfilePresenter(
    private val profileRepository: ProfileRepository,
    private val navigator: Navigator
) : Presenter<EditProfileState> {

    @Composable
    override fun present(): EditProfileState {
        var fullName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var avatarUrl by remember { mutableStateOf<String?>(null) }
        var localImageBytes by remember { mutableStateOf<ByteArray?>(null) }
        var gender by remember { mutableStateOf<Gender?>(null) }
        var dateOfBirth by remember { mutableStateOf<String?>(null) }
        var allergyInput by remember { mutableStateOf("") }
        var allergies by remember { mutableStateOf<List<String>>(emptyList()) }

        var isLoading by remember { mutableStateOf(true) }
        var isSaving by remember { mutableStateOf(false) }
        var isUploadingImage by remember { mutableStateOf(false) }
        var saveError by remember { mutableStateOf<String?>(null) }
        var saved by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            profileRepository.observeProfile().collect { outcome ->
                if (outcome is Outcome.Success) {
                    val p = outcome.data
                    fullName = p.fullName
                    phone = p.phoneNumber ?: ""
                    email = p.email
                    dateOfBirth = p.dateOfBirth
                    allergies = p.allergies ?: emptyList()
                    if (p.avatarUrl != avatarUrl) {
                        localImageBytes = null
                    }
                    avatarUrl = p.avatarUrl
                    gender = p.gender
                    isLoading = false
                }
            }
        }

        return EditProfileState(
            isLoading = isLoading,
            fullName = fullName,
            phoneNumber = phone,
            email = email,
            avatarUrl = avatarUrl,
            localImageBytes = localImageBytes,
            gender = gender,
            dateOfBirth = dateOfBirth,
            allergyInput = allergyInput,
            allergies = allergies,
            isSaving = isSaving,
            isUploadingImage = isUploadingImage,
            saveError = saveError,
            saved = saved,
            eventSink = { event ->
                when (event) {
                    is EditProfileEvent.FullNameChanged -> fullName = event.value
                    is EditProfileEvent.PhoneChanged -> phone = event.value
                    is EditProfileEvent.GenderSelected -> gender = event.value
                    is EditProfileEvent.DateOfBirthChanged -> dateOfBirth = event.value
                    is EditProfileEvent.AllergyInputChanged -> allergyInput = event.value
                    EditProfileEvent.AddAllergy -> {
                        if (allergyInput.isNotBlank()) {
                            allergies = allergies + allergyInput.trim()
                            allergyInput = ""
                        }
                    }
                    is EditProfileEvent.RemoveAllergy -> {
                        allergies = allergies.filter { it != event.allergy }
                    }
                    is EditProfileEvent.ImageSelected -> {
                        localImageBytes = event.bytes
                        scope.launch {
                            isUploadingImage = true
                            saveError = null
                            when (profileRepository.uploadAvatar(event.bytes, "image/jpeg")) {
                                is Outcome.Success -> {
                                    // URL will be updated via profile refresh
                                }
                                is Outcome.Error -> saveError = "Failed to upload image"
                                else -> {}
                            }
                            isUploadingImage = false
                        }
                    }
                    EditProfileEvent.Save -> {
                        scope.launch {
                            isSaving = true
                            saveError = null
                            val patch = ProfileUpdate(
                                fullName = fullName,
                                phoneNumber = phone,
                                gender = gender,
                                dateOfBirth = dateOfBirth,
                                allergies = allergies
                            )
                            when (profileRepository.updateProfile(patch)) {
                                is Outcome.Success -> {
                                    saved = true
                                    navigator.pop()
                                }
                                is Outcome.Error -> saveError = "Failed to save profile"
                                else -> {}
                            }
                            isSaving = false
                        }
                    }
                    EditProfileEvent.Back -> navigator.pop()
                }
            }
        )
    }
}
