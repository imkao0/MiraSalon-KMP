package iz.mkao.mirasalon.core.designsystem.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary

@Composable
actual fun rememberPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): PermissionHandler = remember(onPermissionGranted, onPermissionDenied) {
    object : PermissionHandler {
        private val session = AVAudioSession.sharedInstance()

        override fun requestRecordAudio() {
            session.requestRecordPermission { granted ->
                if (granted) onPermissionGranted() else onPermissionDenied()
            }
        }

        override fun hasRecordAudioPermission(): Boolean =
            when (session.recordPermission) {
                AVAudioSessionRecordPermissionGranted -> true
                AVAudioSessionRecordPermissionDenied -> false
                else -> false // undetermined: not yet granted
            }

        override fun shouldShowRecordAudioRationale(): Boolean =
            session.recordPermission == AVAudioSessionRecordPermissionDenied

        override fun requestGalleryPermission() {
            PHPhotoLibrary.requestAuthorization { status ->
                if (status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited) {
                    onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
        }

        override fun hasGalleryPermission(): Boolean =
            when (PHPhotoLibrary.authorizationStatus()) {
                PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> true
                PHAuthorizationStatusDenied, PHAuthorizationStatusRestricted -> false
                else -> false
            }

        override fun shouldShowGalleryRationale(): Boolean =
            PHPhotoLibrary.authorizationStatus() == PHAuthorizationStatusDenied

        override fun requestCameraPermission() {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                if (granted) onPermissionGranted() else onPermissionDenied()
            }
        }

        override fun hasCameraPermission(): Boolean =
            when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                AVAuthorizationStatusAuthorized -> true
                AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> false
                else -> false
            }

        override fun shouldShowCameraRationale(): Boolean =
            AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusDenied
    }
}
