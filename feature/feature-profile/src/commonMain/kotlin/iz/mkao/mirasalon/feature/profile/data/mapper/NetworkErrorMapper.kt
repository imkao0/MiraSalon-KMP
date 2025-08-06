package iz.mkao.mirasalon.feature.profile.data.mapper

import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.network.result.NetworkError

internal fun NetworkError.toFailure(): Failure = when (this) {
    is NetworkError.NoConnection -> Failure.NetworkConnection(message)
    is NetworkError.Timeout -> Failure.NetworkConnection(message)
    is NetworkError.HttpError -> when (code) {
        401, 403 -> Failure.SessionExpired
        in 400..499 -> Failure.ClientError(code, message)
        else -> Failure.ServerError(code, message)
    }
    is NetworkError.Unknown -> Failure.Unknown
}
