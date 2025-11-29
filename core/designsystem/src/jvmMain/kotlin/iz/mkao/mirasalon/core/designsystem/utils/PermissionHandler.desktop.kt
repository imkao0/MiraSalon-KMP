package iz.mkao.mirasalon.core.designsystem.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): PermissionHandler = remember {
    object : PermissionHandler {
        override fun requestRecordAudio() {
            onPermissionGranted()
        }

        override fun hasRecordAudioPermission(): Boolean = true

        override fun requestGalleryPermission() {
            onPermissionGranted()
        }

        override fun hasGalleryPermission(): Boolean = true

        override fun requestCameraPermission() {
            onPermissionGranted()
        }

        override fun hasCameraPermission(): Boolean = true
    }
}
