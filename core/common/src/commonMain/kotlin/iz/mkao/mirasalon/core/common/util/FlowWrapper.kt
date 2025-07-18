package iz.mkao.mirasalon.core.common.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * A wrapper around Kotlin Flow that simplifies consumption from platform-native
 * code (like Swift/SwiftUI) without complex generic mapping.
 *
 * All subscriptions share a single [CoroutineScope] with a [SupervisorJob].
 * This ensures that cancelling an individual subscription (via the returned [Job])
 * does not affect others, while maintaining structured concurrency.
 *
 * NOTE: The shared `subscriptionScope` is NOT automatically cancelled.
 * Consumers (like SwiftUI view models) MUST cancel the returned [Job] when the
 * UI lifecycle ends to prevent coroutine leaks.
 */
class FlowWrapper<T>(private val flow: Flow<T>) {

    fun watch(onEach: (T) -> Unit): Job =
        flow.onEach { onEach(it) }.launchIn(subscriptionScope)

    private companion object {
        /** Shared scope for all subscriptions; children are independent via SupervisorJob. */
        val subscriptionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}

fun <T> Flow<T>.wrap(): FlowWrapper<T> = FlowWrapper(this)
