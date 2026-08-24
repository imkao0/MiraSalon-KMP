package iz.mkao.mirasalon.feature.profile.presentation.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.PermissionRationaleDialog
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.utils.rememberPermissionHandler
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.feature.profile.domain.model.Gender
import iz.mkao.mirasalon.feature.profile.presentation.circuit.EditProfileEvent
import iz.mkao.mirasalon.feature.profile.presentation.circuit.EditProfileState
import iz.mkao.mirasalon.feature.profile.presentation.util.rememberImagePicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreenContent(
    state: EditProfileState,
    modifier: Modifier = Modifier
) {
    val imagePicker = rememberImagePicker()
    val scope = rememberCoroutineScope()
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionHandler = rememberPermissionHandler(
        onPermissionGranted = {
            scope.launch {
                try {
                    val bytes = imagePicker.pickImage()
                    if (bytes != null) {
                        state.eventSink(EditProfileEvent.ImageSelected(bytes))
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        },
        onPermissionDenied = {
            // Permission denied - in a real app, show a dialog explaining why we need it
        }
    )

    if (showPermissionRationale) {
        PermissionRationaleDialog(
            title = "Gallery Permission Required",
            text = "We need access to your gallery to let you update your profile picture. Please grant the permission to continue.",
            confirmButtonText = "Grant Permission",
            onConfirm = {
                showPermissionRationale = false
                permissionHandler.requestGalleryPermission()
            },
            onDismiss = {
                showPermissionRationale = false
            }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MiraTopAppBar(
                title = "Edit Profile",
                onBackClick = { state.eventSink(EditProfileEvent.Back) },
                actions = {
                    TextButton(
                        onClick = { state.eventSink(EditProfileEvent.Save) },
                        enabled = !state.isSaving && state.fullName.isNotBlank(),
                    ) {
                        if (state.isSaving) {
                            ShimmerLoading(modifier = Modifier.size(20.dp))
                        } else {
                            Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> ShimmerLoading(modifier = Modifier.align(Alignment.Center))
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    state.saveError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Profile Image Section
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val resolvedUrl = ApiEndpoints.resolveImageUrl(state.avatarUrl)
                        if (state.localImageBytes != null || resolvedUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(state.localImageBytes ?: resolvedUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            when {
                                permissionHandler.hasGalleryPermission() -> {
                                    scope.launch {
                                        try {
                                            val bytes = imagePicker.pickImage()
                                            if (bytes != null) {
                                                state.eventSink(EditProfileEvent.ImageSelected(bytes))
                                            }
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                }
                                permissionHandler.shouldShowGalleryRationale() -> {
                                    showPermissionRationale = true
                                }
                                else -> {
                                    permissionHandler.requestGalleryPermission()
                                }
                            }
                        },
                        enabled = !state.isUploadingImage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.isUploadingImage) {
                                ShimmerLoading(modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.isUploadingImage) "Uploading..." else "Update photo", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Form Fields
                    EditProfileTextField(
                        label = "Full Name",
                        value = state.fullName,
                        onValueChange = { state.eventSink(EditProfileEvent.FullNameChanged(it)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    EditProfileTextField(
                        label = "Phone Number",
                        value = state.phoneNumber,
                        onValueChange = { state.eventSink(EditProfileEvent.PhoneChanged(it)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done)
                    )

                    EditProfileTextField(
                        label = "Email Address",
                        value = state.email,
                        onValueChange = { },
                        enabled = false
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Gender",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Gender.entries.forEach { gender ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        state.eventSink(EditProfileEvent.GenderSelected(gender))
                                    }
                                ) {
                                    RadioButton(
                                        selected = state.gender == gender,
                                        onClick = { state.eventSink(EditProfileEvent.GenderSelected(gender)) }
                                    )
                                    Text(
                                        text = gender.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun EditProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardOptions = if (keyboardOptions.imeAction == ImeAction.Default) {
                keyboardOptions.copy(imeAction = ImeAction.Next)
            } else {
                keyboardOptions
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}
