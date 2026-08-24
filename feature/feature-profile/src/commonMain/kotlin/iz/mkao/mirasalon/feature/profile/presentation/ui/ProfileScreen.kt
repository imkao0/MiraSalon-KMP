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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.RectangularSwitch
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeIntermediate
import iz.mkao.mirasalon.core.designsystem.theme.IconSizeMedium
import iz.mkao.mirasalon.core.designsystem.theme.ProfileAvatarSize
import iz.mkao.mirasalon.core.designsystem.theme.RadiusSmall
import iz.mkao.mirasalon.core.designsystem.theme.RadiusProfileCard
import iz.mkao.mirasalon.core.designsystem.theme.SpacingDefault
import iz.mkao.mirasalon.core.designsystem.theme.SpacingExtraLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall
import iz.mkao.mirasalon.core.designsystem.theme.SpacingTiny
import iz.mkao.mirasalon.feature.profile.domain.model.AppTheme
import iz.mkao.mirasalon.feature.profile.presentation.circuit.ProfileEvent
import iz.mkao.mirasalon.feature.profile.presentation.circuit.ProfileState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MiraTopAppBar(title = "Settings", onBackClick = {}) }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    ShimmerLoading()
                }
            }
            state.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { state.eventSink(ProfileEvent.Retry) }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Spacer(modifier = Modifier.height(SpacingMedium))
                        ProfileCard {
                            ProfileHeader(
                                name = state.profile?.fullName.orEmpty(),
                                email = state.profile?.email.orEmpty(),
                                avatarUrl = state.profile?.avatarUrl,
                                onEditProfile = { state.eventSink(ProfileEvent.EditProfile) }
                            )
                        }
                    }

                    item { SectionHeader("Account details") }
                    item {
                        ProfileCard {
                            Column {
                                ProfileMenuRow(
                                    icon = Icons.Outlined.Person,
                                    label = "Account Info",
                                    onClick = { state.eventSink(ProfileEvent.EditProfile) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = SpacingMedium), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ProfileMenuRow(
                                    icon = Icons.Outlined.LocationOn,
                                    label = "Saved Addresses",
                                    trailingText = state.addressCount.takeIf { it > 0 }?.toString(),
                                    onClick = { state.eventSink(ProfileEvent.SavedAddresses) },
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = SpacingMedium), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ProfileMenuRow(
                                    icon = Icons.Outlined.Schedule,
                                    label = "Appointments",
                                    badgeCount = state.upcomingRemindersCount,
                                    onClick = { state.eventSink(ProfileEvent.MyAppointments) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = SpacingMedium), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ProfileMenuRow(
                                    icon = Icons.Outlined.Description,
                                    label = "Orders",
                                    onClick = { state.eventSink(ProfileEvent.MyOrders) }
                                )
                            }
                        }
                    }

                    item { SectionHeader("Preferences") }
                    item {
                        ProfileCard {
                            Column {
                                ProfileMenuRow(icon = Icons.Outlined.Favorite, label = "Favourites", onClick = { state.eventSink(ProfileEvent.Favourites) })
                                HorizontalDivider(modifier = Modifier.padding(horizontal = SpacingMedium), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ProfileMenuRow(
                                    icon = Icons.Outlined.Notifications,
                                    label = "In-app Notifications",
                                    onClick = { },
                                    trailingContent = {
                                        RectangularSwitch(
                                            checked = state.inAppNotificationsEnabled,
                                            onCheckedChange = { enabled ->
                                                state.eventSink(ProfileEvent.ToggleInAppNotifications(enabled))
                                            }
                                        )
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = SpacingMedium), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ProfileMenuRow(icon = Icons.Outlined.Language, label = "Language", onClick = {})
                                HorizontalDivider(modifier = Modifier.padding(horizontal = SpacingMedium), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ProfileMenuRow(
                                    icon = Icons.Outlined.Settings,
                                    label = "Light/Dark Mode",
                                    onClick = { },
                                    trailingContent = {
                                        RectangularSwitch(
                                            checked = state.currentTheme == AppTheme.DARK,
                                            onCheckedChange = { isDark ->
                                                state.eventSink(ProfileEvent.SetTheme(if (isDark) AppTheme.DARK else AppTheme.LIGHT))
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    item { SectionHeader("Support") }
                    item {
                        ProfileCard {
                            ProfileMenuRow(
                                icon = Icons.AutoMirrored.Outlined.Logout,
                                label = "Log out",
                                tint = MaterialTheme.colorScheme.error,
                                onClick = { state.eventSink(ProfileEvent.Logout) },
                                showChevron = false
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(SpacingExtraLarge)) }
                }
            }
        }
    }
}

@Composable
fun ProfileScreenWrapper(
    state: ProfileState,
    modifier: Modifier = Modifier
) {
    ProfileScreen(state, modifier)
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge, vertical = SpacingDefault),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun ProfileCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingMedium),
        shape = RoundedCornerShape(RadiusProfileCard),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        content()
    }
}

@Composable
private fun ProfileHeader(name: String, email: String, avatarUrl: String?, onEditProfile: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(ProfileAvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val resolvedUrl = ApiEndpoints.resolveImageUrl(avatarUrl)
            Napier.d(tag = "ProfileScreen") { "avatarUrl: $avatarUrl, resolved: $resolvedUrl" }
            if (resolvedUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(resolvedUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { result ->
                        Napier.e(tag = "ProfileScreen") { "Image load error: ${result.result.throwable}" }
                    }
                )
            } else {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(IconSizeIntermediate),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.padding(start = SpacingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(name.ifBlank { "Your name" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(SpacingTiny))
            Row(
                modifier = Modifier.clickable(onClick = onEditProfile),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(SpacingTiny))
                Text(
                    "Edit Profile",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    label: String,
    trailingText: String? = null,
    badgeCount: Int = 0,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = SpacingMedium, vertical = SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(IconSizeMedium), tint = tint)
            Spacer(modifier = Modifier.padding(start = SpacingMedium))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badgeCount > 0) {
                Badge { Text(badgeCount.toString()) }
                Spacer(modifier = Modifier.padding(start = SpacingSmall))
            }
            trailingText?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.padding(start = SpacingSmall))
            }
            trailingContent?.let {
                it()
                Spacer(modifier = Modifier.padding(start = SpacingSmall))
            }
            if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
