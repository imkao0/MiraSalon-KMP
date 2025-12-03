package iz.mkao.mirasalon.core.common.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Dispatchers.IO is internal on Kotlin/Native; Default is the closest IO-ish pool.
actual fun createIoDispatcher(): CoroutineDispatcher = Dispatchers.Default
