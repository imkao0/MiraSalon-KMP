package iz.mkao.mirasalon.feature.profile.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val bytes = outputStream.toByteArray()
                outputStream.close()
                
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
    
    fun launchPicker(launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>) {
        val intent = android.content.Intent(android.content.Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        launcher.launch(intent)
    }
}

@Composable
actual fun rememberImagePicker(): ImagePicker {
    val context = LocalContext.current
    val imagePicker = remember { AndroidImagePicker(context) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            imagePicker.onImageSelected(result.data?.data)
        } else {
            imagePicker.onImageCancelled()
        }
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
