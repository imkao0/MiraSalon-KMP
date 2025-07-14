package iz.mkao.mirasalon.core.common.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

/**
 * Provides the platform IO dispatcher. `Dispatchers.IO` only exists on the JVM;
 * on Kotlin/Native it is `internal` and unusable, so each platform supplies its
 * own IO-ish dispatcher (Native falls back to [Dispatchers.Default]).
 */
expect fun createIoDispatcher(): CoroutineDispatcher

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = createIoDispatcher()
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
