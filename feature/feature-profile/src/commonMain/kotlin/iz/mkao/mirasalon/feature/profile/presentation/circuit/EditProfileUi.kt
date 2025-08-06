package iz.mkao.mirasalon.feature.profile.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.navigation.ProfileRoute
import iz.mkao.mirasalon.feature.profile.presentation.ui.EditProfileScreenContent

@Composable
fun EditProfileUi(state: EditProfileState, modifier: Modifier = Modifier) {
    EditProfileScreenContent(state, modifier)
}

