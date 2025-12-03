package iz.mkao.mirasalon.core.network.mapper

import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.result.NetworkError
import iz.mkao.mirasalon.core.network.result.NetworkResult

/**
 * Maps NetworkError to Domain Failure
 */
fun NetworkError.toFailure(): Failure = when (this) {
    is NetworkError.NoConnection, is NetworkError.Timeout -> Failure.NetworkConnection(message)
    is NetworkError.HttpError -> when (code) {
        401, 403 -> Failure.SessionExpired
        in 400..499 -> Failure.ClientError(code, message)
        in 500..599 -> Failure.ServerError(code, message)
        else -> Failure.Unknown
    }
    is NetworkError.Unknown -> Failure.Unknown
}

/**
 * Maps NetworkResult to Domain Outcome with data transformation
 */
fun <T, R> NetworkResult<T>.toOutcome(mapper: (T) -> R): Outcome<R> {
    return when (this) {
        is NetworkResult.Success -> Outcome.Success(mapper(data))
        is NetworkResult.Error -> Outcome.Error(error.toFailure())
    }
}

/**
 * Maps NetworkResult to Domain Outcome without data transformation
 */
fun <T> NetworkResult<T>.toOutcome(): Outcome<T> {
    return when (this) {
        is NetworkResult.Success -> Outcome.Success(data)
        is NetworkResult.Error -> Outcome.Error(error.toFailure())
    }
}
