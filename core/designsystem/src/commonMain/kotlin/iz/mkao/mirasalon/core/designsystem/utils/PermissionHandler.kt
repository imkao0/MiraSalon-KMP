package iz.mkao.mirasalon.core.designsystem.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): PermissionHandler

interface PermissionHandler {
    fun requestRecordAudio()
    fun hasRecordAudioPermission(): Boolean

    fun requestGalleryPermission()
    fun hasGalleryPermission(): Boolean

    fun requestCameraPermission()
    fun hasCameraPermission(): Boolean
}
