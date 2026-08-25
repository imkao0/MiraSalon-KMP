package iz.mkao.mirasalon.core.common.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class SnackbarEvent(
    val message: String,
    val action: SnackbarAction? = null
)

data class SnackbarAction(
    val label: String,
    val action: () -> Unit
)

object SnackbarController {
    private val _events = MutableSharedFlow<SnackbarEvent>()
    val events = _events.asSharedFlow()

    suspend fun showSnackbar(
        message: String,
        action: SnackbarAction? = null
    ) {
        _events.emit(SnackbarEvent(message, action))
    }
}
