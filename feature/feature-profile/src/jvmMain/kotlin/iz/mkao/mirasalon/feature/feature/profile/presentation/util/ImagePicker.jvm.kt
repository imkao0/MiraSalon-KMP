package iz.mkao.mirasalon.feature.profile.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class JvmImagePicker : ImagePicker {
    override suspend fun pickImage(): ByteArray? = withContext(Dispatchers.IO) {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif")
            dialogTitle = "Select Profile Image"
        }
        
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile?.readBytes()
        } else {
            null
        }
    }
}

@Composable
actual fun rememberImagePicker(): ImagePicker = remember { JvmImagePicker() }
