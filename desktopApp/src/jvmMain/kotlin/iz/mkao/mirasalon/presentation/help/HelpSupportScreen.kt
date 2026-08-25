package iz.mkao.mirasalon.presentation.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import iz.mkao.mirasalon.core.designsystem.theme.MiraBackground
import iz.mkao.mirasalon.core.designsystem.theme.MiraCoral
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextPrimary
import iz.mkao.mirasalon.core.designsystem.theme.MiraTextSecondary
import iz.mkao.mirasalon.presentation.DesktopScreen
import iz.mkao.mirasalon.presentation.LocalDesktopNavigate
import iz.mkao.mirasalon.presentation.LocalSidebarExpanded
import iz.mkao.mirasalon.presentation.LocalToggleSidebar
import iz.mkao.mirasalon.presentation.components.DesktopShell
import iz.mkao.mirasalon.presentation.dashboard.components.Sidebar

data class HelpUiState(
    val eventSink: (HelpEvent) -> Unit = {}
) : CircuitUiState

sealed interface HelpEvent : CircuitUiEvent

class HelpPresenter : Presenter<HelpUiState> {
    @Composable
    override fun present(): HelpUiState {
        return HelpUiState { }
    }
}

@Composable
fun HelpSupportScreenUi(
    state: HelpUiState,
    modifier: Modifier = Modifier
) {
    DesktopShell(
        title = "Help & Support",
        selectedRoute = "Help"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MiraCoral.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "How can we help you?",
                style = MaterialTheme.typography.headlineMedium,
                color = MiraTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Our support team is here to assist you with any questions.",
                style = MaterialTheme.typography.bodyLarge,
                color = MiraTextSecondary
            )
        }
    }
}

class HelpUiFactory : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is DesktopScreen.Help -> ui<HelpUiState> { state, _ ->
            HelpSupportScreenUi(
                state = state
            )
        }
        else -> null
    }
}
