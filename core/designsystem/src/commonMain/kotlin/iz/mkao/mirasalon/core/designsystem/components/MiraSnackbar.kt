package iz.mkao.mirasalon.core.designsystem.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.theme.ElevationMedium
import iz.mkao.mirasalon.core.designsystem.theme.RadiusExtraLarge
import iz.mkao.mirasalon.core.designsystem.theme.SnackbarMaxWidth
import iz.mkao.mirasalon.core.designsystem.theme.SpacingMedium
import iz.mkao.mirasalon.core.designsystem.theme.SpacingSmall

@Composable
fun MiraSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val visuals = snackbarData.visuals

    Surface(
        modifier = modifier.widthIn(max = SnackbarMaxWidth),
        shape = RoundedCornerShape(RadiusExtraLarge),
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = ElevationMedium,
    ) {
        Row(
            modifier = Modifier.padding(PaddingValues(horizontal = SpacingMedium, vertical = SpacingSmall + 2.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )

            visuals.actionLabel?.let { action ->
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.inversePrimary,
                        ),
                ) {
                    Text(action)
                }
            }

            if (visuals.withDismissAction) {
                IconButton(onClick = { snackbarData.dismiss() }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            }
        }
    }
}
