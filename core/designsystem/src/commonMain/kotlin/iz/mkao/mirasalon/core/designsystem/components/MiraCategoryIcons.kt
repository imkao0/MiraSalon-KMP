package iz.mkao.mirasalon.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BackHand
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DryCleaning
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter

/**
 * Maps a service-category key to the matching Material Outlined icon.
 */
fun categoryIconVector(key: String?): ImageVector {
    val k =
        key
            ?.lowercase()
            ?.replace("content_cut", "haircut")
            ?.replace("palette", "coloring")
            ?.replace("back_hand", "nails")
            ?.replace("brush", "makeup")
            ?.replace("face", "styling")
            ?.trim()
    return when {
        k == null -> Icons.Outlined.GridView
        "nail" in k || "manicure" in k || "pedicure" in k -> Icons.Outlined.BackHand
        "makeup" in k || "make-up" in k -> Icons.Outlined.AutoAwesome
        "wax" in k || "thread" in k || "hair removal" in k || "removal" in k -> Icons.Outlined.DryCleaning
        "skin" in k || "facial" in k || "spa" in k || "massage" in k -> Icons.Outlined.Spa
        "color" in k || "dye" in k -> Icons.Outlined.Palette
        "hair" in k || "cut" in k || "styl" in k || "barber" in k || "braid" in k -> Icons.Outlined.ContentCut
        else -> Icons.Outlined.GridView
    }
}

/** Composable variant of [categoryIconVector] returning a [Painter]. */
@Composable
fun categoryIconPainter(key: String?): Painter = rememberVectorPainter(categoryIconVector(key))
