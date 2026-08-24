package iz.mkao.mirasalon.core.designsystem.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import iz.mkao.mirasalon.core.designsystem.theme.*

@Composable
fun MiraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(ButtonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(RadiusSmall),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingDefault),
        content = content,
    )
}

@Composable
fun MiraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MiraButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        MiraButtonText(text)
    }
}

@Composable
fun MiraOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(ButtonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(RadiusSmall),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        border = ButtonDefaults.outlinedButtonBorder(enabled),
        contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingDefault),
        content = content,
    )
}

@Composable
fun MiraOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MiraOutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        MiraButtonText(text)
    }
}

@Composable
private fun RowScope.MiraButtonText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    )
}

@Preview(showBackground = true)
@Composable
private fun MiraButtonPreview() {
    MiraButton(text = "Primary", onClick = {}, modifier = Modifier.fillMaxWidth())
}

@Preview(showBackground = true)
@Composable
private fun MiraButtonDisabledPreview() {
    MiraButton(text = "Disabled", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
}

@Preview(showBackground = true)
@Composable
private fun MiraOutlinedButtonPreview() {
    MiraOutlinedButton(text = "Outlined", onClick = {}, modifier = Modifier.fillMaxWidth())
}

@Preview(showBackground = true)
@Composable
private fun MiraOutlinedButtonDisabledPreview() {
    MiraOutlinedButton(text = "Disabled", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
}
