package iz.mkao.mirasalon.core.common.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun createIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
