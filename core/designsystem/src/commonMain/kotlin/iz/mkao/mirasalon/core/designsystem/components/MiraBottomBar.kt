package iz.mkao.mirasalon.core.designsystem.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.theme.BottomNavHeight
import iz.mkao.mirasalon.core.designsystem.theme.BottomNavIconSize

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val id: Any,
    val badgeCount: Int = 0,
)

@Composable
fun SnapBottomBar(
    items: List<BottomNavItem>,
    selectedItemId: Any?,
    onItemClick: (Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.height(BottomNavHeight),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        items.forEach { item ->
            val isSelected = selectedItemId == item.id
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item.id) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.badgeCount > 0) {
                                Text(
                                    text = item.badgeCount.toString(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(BottomNavIconSize),
                        )
                    }
                },
                alwaysShowLabel = false,
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent,
                    ),
            )
        }
    }
}
