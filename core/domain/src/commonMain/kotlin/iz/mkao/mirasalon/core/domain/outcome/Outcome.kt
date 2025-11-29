package iz.mkao.mirasalon.core.domain.outcome

/**
 * Domain Result Wrapper
 */
sealed class Outcome<out T> {
    data class Success<out T>(val data: T) : Outcome<T>()
    data class Error(val failure: Failure) : Outcome<Nothing>()
    data object Loading : Outcome<Nothing>()

    inline fun <R> map(transform: (T) -> R): Outcome<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(failure)
        Loading -> Loading
    }
}

/**
 * Granular Domain Errors for UI consumption
 */
sealed class Failure {
    data class NetworkConnection(val message: String) : Failure()
    data class ServerError(val code: Int, val message: String) : Failure()
    data class ClientError(val code: Int, val message: String) : Failure()
    data object SessionExpired : Failure() // 401/403
    data object Unknown : Failure()
}
