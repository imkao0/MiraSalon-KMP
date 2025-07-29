package iz.mkao.mirasalon.feature.profile.presentation.util

import androidx.compose.runtime.Composable

interface ImagePicker {
    suspend fun pickImage(): ByteArray?
}

@Composable
expect fun rememberImagePicker(): ImagePicker

