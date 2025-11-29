package iz.mkao.mirasalon.core.network.result

sealed interface SalonNetworkResult<out T> {
    data class Success<out T>(val data: T) : SalonNetworkResult<T>
    data class Failure(val error: SalonError) : SalonNetworkResult<Nothing>
}

inline fun <T, R> SalonNetworkResult<T>.map(transform: (T) -> R): SalonNetworkResult<R> = when (this) {
    is SalonNetworkResult.Success -> SalonNetworkResult.Success(transform(data))
    is SalonNetworkResult.Failure -> this
}

inline fun <T> SalonNetworkResult<T>.onSuccess(action: (T) -> Unit): SalonNetworkResult<T> {
    if (this is SalonNetworkResult.Success) action(data)
    return this
}

inline fun <T> SalonNetworkResult<T>.onFailure(action: (SalonError) -> Unit): SalonNetworkResult<T> {
    if (this is SalonNetworkResult.Failure) action(error)
    return this
}

fun <T> SalonNetworkResult<T>.getOrNull(): T? = (this as? SalonNetworkResult.Success)?.data

fun <T> SalonNetworkResult<T>.getOrThrow(): T = when (this) {
    is SalonNetworkResult.Success -> data
    is SalonNetworkResult.Failure -> throw SalonApiException(error)
}
