package iz.mkao.mirasalon.feature.auth.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.designsystem.Res
import iz.mkao.mirasalon.core.designsystem.continue_as_guest
import iz.mkao.mirasalon.core.designsystem.login
import iz.mkao.mirasalon.core.designsystem.register
import iz.mkao.mirasalon.core.designsystem.slogan
import iz.mkao.mirasalon.core.designsystem.theme.ButtonHeight
import iz.mkao.mirasalon.core.designsystem.theme.RadiusSmall
import iz.mkao.mirasalon.core.designsystem.theme.SpacingExtraLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingLarge
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.StrokeThin
import iz.mkao.mirasalon.feature.auth.presentation.circuit.AuthEvent
import iz.mkao.mirasalon.feature.auth.presentation.circuit.AuthState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeScreen(state: AuthState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                painter = painterResource(Res.drawable.slogan),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingLarge, vertical = SpacingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { state.eventSink(AuthEvent.NavigateToLogin) },
                modifier = Modifier.fillMaxWidth().height(ButtonHeight),
                shape = RoundedCornerShape(RadiusSmall),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(Res.string.login), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(SpacingMedium))
            OutlinedButton(
                onClick = { state.eventSink(AuthEvent.NavigateToRegister) },
                modifier = Modifier.fillMaxWidth().height(ButtonHeight),
                shape = RoundedCornerShape(RadiusSmall),
                border = androidx.compose.foundation.BorderStroke(StrokeThin, MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(Res.string.register), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(SpacingLarge))
            TextButton(onClick = { state.eventSink(AuthEvent.ContinueAsGuest) }) {
                Text(
                    stringResource(Res.string.continue_as_guest),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
