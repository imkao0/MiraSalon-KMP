package iz.mkao.mirasalon.feature.salon.services.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import iz.mkao.mirasalon.core.designsystem.components.categoryIconPainter

/**
 * @deprecated Service/category icons now come from the design system via
 * [categoryIconPainter]. This shim only remains for binary compatibility and
 * should not be used in new code.
 */
@Deprecated(
    message = "Use categoryIconPainter from the design system instead",
    replaceWith = ReplaceWith(
        "categoryIconPainter(iconName)",
        "iz.mkao.mirasalon.core.designsystem.components.categoryIconPainter"
    )
)
@Composable
fun getServiceIconPainter(iconName: String?): Painter = categoryIconPainter(iconName)
