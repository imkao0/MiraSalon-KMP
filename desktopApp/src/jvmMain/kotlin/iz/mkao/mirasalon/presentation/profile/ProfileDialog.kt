package iz.mkao.mirasalon.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.domain.repository.UploadRepository
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.data.local.TokenManager
import iz.mkao.mirasalon.data.remote.AuthClient
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun ProfileDialog(
    tokenManager: TokenManager,
    authClient: AuthClient,
    uploadRepository: UploadRepository,
    onDismiss: () -> Unit
) {
    val presenter = remember {
        ProfileDialogPresenter(tokenManager, authClient, uploadRepository, onDismiss)
    }
    val state = presenter.present()

    ProfileDialogUi(state)
}

@Composable
fun ProfileDialogUi(state: ProfileDialogState) {
    Dialog(onDismissRequest = { state.eventSink(ProfileDialogEvent.Dismiss) }) {
        Surface(
            modifier = Modifier.width(450.dp).padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Admin Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiraTextPrimary
                    )
                    IconButton(onClick = { state.eventSink(ProfileDialogEvent.Dismiss) }) {
                        Icon(Icons.Outlined.Close, null)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable {
                            val chooser = JFileChooser()
                            chooser.fileFilter = FileNameExtensionFilter(
                                "Images (jpg, png, webp)", "jpg", "jpeg", "png", "webp"
                            )
                            val result = chooser.showOpenDialog(null)
                            if (result == JFileChooser.APPROVE_OPTION) {
                                val file = chooser.selectedFile
                                state.eventSink(ProfileDialogEvent.ImageSelected(file.readBytes(), file.name))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.avatarUrl != null || state.selectedImageBytes != null) {
                        if (state.selectedImageBytes != null) {
                            Icon(Icons.Outlined.Check, null, modifier = Modifier.size(40.dp), tint = Color.White)
                        } else {
                            state.avatarUrl?.let { avatarUrl ->
                                val fullUrl = ApiEndpoints.resolveImageUrl(avatarUrl)
                                AsyncImage(
                                    model = fullUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    onError = {
                                        Napier.e(it.result.throwable) { "Coil failed to load profile image: $fullUrl" }
                                    }
                                )
                            }
                        }
                    } else {
                        Icon(
                            Icons.Outlined.Person,
                            null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.White
                        )
                    }

                    if (state.isLoading && state.selectedImageName != null) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ShimmerLoading(
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Click to change photo", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.firstName,
                        onValueChange = { state.eventSink(ProfileDialogEvent.FirstNameChanged(it)) },
                        label = { Text("First Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = state.lastName,
                        onValueChange = { state.eventSink(ProfileDialogEvent.LastNameChanged(it)) },
                        label = { Text("Last Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { state.eventSink(ProfileDialogEvent.PhoneChanged(it)) },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.address,
                    onValueChange = { state.eventSink(ProfileDialogEvent.AddressChanged(it)) },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(2.dp)
                )

                val currentMessage = state.message
                if (currentMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        currentMessage,
                        color = if (currentMessage.contains("Successfully", ignoreCase = true)) MaterialTheme.colorScheme.tertiary else MiraCoral,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { state.eventSink(ProfileDialogEvent.SaveClicked) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MiraCoral),
                    shape = RoundedCornerShape(2.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        ShimmerLoading(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
