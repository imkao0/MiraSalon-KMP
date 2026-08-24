package iz.mkao.mirasalon.feature.profile.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol as PHPickerViewControllerDelegate

@OptIn(ExperimentalForeignApi::class)
class IosImagePicker : ImagePicker {
    private var viewController: UIViewController? = null
    private var currentDelegate: PHPickerViewControllerDelegate? = null
    
    fun setViewController(vc: UIViewController) {
        viewController = vc
    }
    
    override suspend fun pickImage(): ByteArray? = suspendCancellableCoroutine { continuation ->
        val vc = viewController ?: return@suspendCancellableCoroutine continuation.resumeWithException(
            IllegalStateException("UIViewController not set")
        )
        
        val config = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
        }
        
        val picker = PHPickerViewController(configuration = config)
        
        val delegate = object : NSObject(), PHPickerViewControllerDelegate {
            override fun picker(
                picker: PHPickerViewController,
                didFinishPicking: List<*>
            ) {
                picker.dismissViewControllerAnimated(true) {
                    currentDelegate = null
                    val results = didFinishPicking as? List<PHPickerResult>
                    val result = results?.firstOrNull()
                    
                    if (result != null) {
                        val itemProvider = result.itemProvider
                        if (itemProvider.hasItemConformingToTypeIdentifier("public.image")) {
                            itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, error ->
                                if (error != null) {
                                    continuation.resumeWithException(Exception(error.localizedDescription))
                                    return@loadDataRepresentationForTypeIdentifier
                                }
                                
                                val bytes = data?.let { nsData ->
                                    ByteArray(nsData.length.toInt()).apply {
                                        usePinned { pinned ->
                                            memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
                                        }
                                    }
                                }
                                continuation.resume(bytes)
                            }
                        } else {
                            continuation.resume(null)
                        }
                    } else {
                        continuation.resume(null)
                    }
                }
            }
        }
        
        currentDelegate = delegate
        picker.delegate = delegate
        
        continuation.invokeOnCancellation {
            picker.dismissViewControllerAnimated(true) {
                currentDelegate = null
            }
        }
        
        vc.presentViewController(picker, true) {}
    }
}

@Composable
actual fun rememberImagePicker(): ImagePicker {
    val viewController = LocalUIViewController.current
    val imagePicker = remember { IosImagePicker() }
    
    // Update the view controller whenever it changes
    imagePicker.setViewController(viewController)
    
    return imagePicker
}
