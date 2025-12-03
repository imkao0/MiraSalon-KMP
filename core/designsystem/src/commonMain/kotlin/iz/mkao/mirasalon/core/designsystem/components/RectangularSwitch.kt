package iz.mkao.mirasalon.core.designsystem.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.theme.RadiusMedium

@Composable
fun RectangularSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateDpAsState(if (checked) 24.dp else 0.dp)
    val trackColor = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier =
            modifier
                .width(48.dp)
                .height(24.dp)
                .background(trackColor, RoundedCornerShape(RadiusMedium))
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = Role.Switch,
                ).padding(2.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .offset(x = thumbOffset)
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(2.dp)),
        )
    }
}
