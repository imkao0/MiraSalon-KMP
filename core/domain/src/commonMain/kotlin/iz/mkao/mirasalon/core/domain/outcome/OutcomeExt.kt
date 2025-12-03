package iz.mkao.mirasalon.core.domain.outcome

import iz.mkao.mirasalon.core.common.error.NetworkException
import iz.mkao.mirasalon.core.common.result.NetworkResult

fun <T> Outcome<T>.toNetworkResult(): NetworkResult<T> = when (this) {
    is Outcome.Success -> NetworkResult.Success(data)
    is Outcome.Error -> NetworkResult.Error(failure.toThrowable())
    Outcome.Loading -> NetworkResult.Loading
}

fun Failure.toThrowable(): Throwable = when (this) {
    is Failure.NetworkConnection -> NetworkException.NoConnection(message)
    is Failure.ServerError -> NetworkException.ServerError(code, message)
    is Failure.ClientError -> NetworkException.ServerError(code, message)
    Failure.SessionExpired -> NetworkException.ServerError(401, "Session expired")
    Failure.Unknown -> NetworkException.Unknown()
}
