package iz.mkao.mirasalon.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.designsystem.theme.*
import iz.mkao.mirasalon.presentation.*
import iz.mkao.mirasalon.presentation.components.DesktopShell

@Composable
fun SettingsScreenUi(
    state: SettingsUiState,
    modifier: Modifier = Modifier
) {
    val salon = state.salon
    val isLoading = state.isLoading

    var firstName by remember(salon) { mutableStateOf(salon?.name?.split(" ")?.firstOrNull() ?: "") }
    var lastName by remember(salon) {
        mutableStateOf(salon?.name?.split(" ")?.drop(1)?.joinToString(" ") ?: "")
    }
    var phone by remember(salon) { mutableStateOf(salon?.phone ?: "") }
    var email by remember(salon) { mutableStateOf("") }
    var address by remember(salon) { mutableStateOf(salon?.address ?: "") }
    var openTime by remember(salon) { mutableStateOf(salon?.openTime ?: "08:00") }
    var closeTime by remember(salon) { mutableStateOf(salon?.closeTime ?: "17:00") }
    var timezoneId by remember(salon) { mutableStateOf(salon?.timezoneId ?: "UTC") }

    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabs = listOf("Account")

    DesktopShell(
        title = "Settings",
        selectedRoute = "Settings"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    OutlinedButton(
                        onClick = { /* Discard */ },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MiraBorder),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("Discard", color = MiraTextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            salon?.let {
                                state.eventSink(SettingsEvent.UpdateSalon(it.copy(
                                    name = "$firstName $lastName",
                                    phone = phone,
                                    address = address,
                                    openTime = openTime,
                                    closeTime = closeTime,
                                    timezoneId = timezoneId
                                )))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MiraCoral),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            ShimmerLoading(modifier = Modifier.size(20.dp))
                        } else {
                            Text("Update", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MiraCoral,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MiraCoral
                    )
                },
                modifier = Modifier.width(400.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                fontSize = 16.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) MiraCoral else MiraTextSecondary
                            )
                        }
                    )
                }
            }

            HorizontalDivider(color = MiraBorder, thickness = 1.dp)

            Spacer(modifier = Modifier.height(32.dp))

            // My Profile Section
            Text("My profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MiraTextPrimary)
            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MiraBorder.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, null, modifier = Modifier.size(32.dp), tint = MiraTextSecondary)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    OutlinedButton(
                        onClick = { /* Upload */ },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MiraCoral.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MiraCoral)
                    ) {
                        Text("Upload picture", fontSize = 14.sp)
                    }
                    Text(
                        "Jpg, GIF or PNG. Recommended size 256x256 px",
                        fontSize = 12.sp,
                        color = MiraTextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Fields Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                SettingField(
                    label = "First name",
                    value = firstName,
                    onValueChange = { firstName = it },
                    placeholder = "Enter first name",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(24.dp))
                SettingField(
                    label = "Last name",
                    value = lastName,
                    onValueChange = { lastName = it },
                    placeholder = "Enter last name",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SettingField(
                    label = "Account email",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Enter email",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(24.dp))
                SettingField(
                    label = "Salon Address",
                    value = address,
                    onValueChange = { address = it },
                    placeholder = "Enter address",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mobile phone number", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MiraTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = MiraCoral,
                            unfocusedIndicatorColor = MiraBorder
                        )
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                Box(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Working Hours Section
            Text("Operating Hours", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MiraTextPrimary)
            Text(
                "These hours define the default appointment slots generated for specialists.",
                fontSize = 14.sp,
                color = MiraTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SettingField(
                    label = "Opens At",
                    value = openTime,
                    onValueChange = { openTime = it },
                    placeholder = "08:00",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(24.dp))
                SettingField(
                    label = "Closes At",
                    value = closeTime,
                    onValueChange = { closeTime = it },
                    placeholder = "17:00",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SettingField(
                    label = "Timezone ID",
                    value = timezoneId,
                    onValueChange = { timezoneId = it },
                    placeholder = "UTC",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(24.dp))
                Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SettingField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MiraTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = MiraTextSecondary.copy(alpha = 0.5f)) },
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = MiraCoral,
                unfocusedIndicatorColor = MiraBorder
            ),
            singleLine = true
        )
    }
}


class SettingsUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Settings -> ui<SettingsUiState> { state, _ ->
            SettingsScreenUi(
                state = state
            )
        }
        else -> null
    }
}
