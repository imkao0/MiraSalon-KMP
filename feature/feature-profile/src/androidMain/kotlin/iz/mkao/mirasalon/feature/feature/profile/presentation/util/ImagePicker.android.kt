package iz.mkao.mirasalon.feature.profile.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

open class AndroidImagePicker(private val context: Context) : ImagePicker {
    private var continuation: CancellableContinuation<ByteArray?>? = null
    
    fun onImageSelected(uri: Uri?) {
        val cont = continuation
        continuation = null
        
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                if (bitmap == null) {
                    cont?.resume(null)
                    return
                }

                // Resize if too large
                val maxWidth = 1024
                val maxHeight = 1024
                val ratio = Math.min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
                val resizedBitmap = if (ratio < 1f) {
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                } else {
                    bitmap
                }
                
                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val bytes = outputStream.toByteArray()
                outputStream.close()
                
                if (resizedBitmap != bitmap) {
                    resizedBitmap.recycle()
                }
                bitmap.recycle()
                
                cont?.resume(bytes)
            } catch (e: Exception) {
                cont?.resumeWithException(e)
            }
        } else {
            cont?.resume(null)
        }
    }
    
    fun onImageCancelled() {
        val cont = continuation
        continuation = null
        cont?.resume(null)
    }
    
    override suspend fun pickImage(): ByteArray? = suspendCancellableCoroutine { cont ->
        continuation = cont
        cont.invokeOnCancellation {
            continuation = null
        }
    }
    
    fun launchPicker(launcher: ActivityResultLauncher<PickVisualMediaRequest>) {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

@Composable
actual fun rememberImagePicker(): ImagePicker {
    val context = LocalContext.current
    val imagePicker = remember { AndroidImagePicker(context) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        imagePicker.onImageSelected(uri)
    }
    
    return remember {
        object : ImagePicker {
            override suspend fun pickImage(): ByteArray? {
                imagePicker.launchPicker(launcher)
                return imagePicker.pickImage()
            }
        }
    }
}
