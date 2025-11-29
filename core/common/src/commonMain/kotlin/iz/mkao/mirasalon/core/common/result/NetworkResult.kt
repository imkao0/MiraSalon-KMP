package iz.mkao.mirasalon.core.common.result

/**
 * Throwable-based result type used by the presentation layer.
 *
 * Unlike `core.network.result.NetworkResult` (which carries a typed
 * `NetworkError`), this variant carries the raw [Throwable] so the presentation
 * layer can inspect specific exception types (e.g. `NetworkException`) and map them to
 * user-facing messages. Expected failures are values, not thrown exceptions.
 */
sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()

    data class Error(val error: Throwable) : NetworkResult<Nothing>()

    data object Loading : NetworkResult<Nothing>()

    /** True when this result is a [Success]. */
    val isSuccess: Boolean
        get() = this is Success

    /** Returns the success payload or null when this is an [Error]. */
    fun getOrNull(): T? = (this as? Success)?.data

    fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(error)
        Loading -> Loading
    }

    companion object {
        fun <T> success(data: T): NetworkResult<T> = Success(data)

        fun error(error: Throwable): NetworkResult<Nothing> = Error(error)
    }
}
