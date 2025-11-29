package iz.mkao.mirasalon.core.network.result

sealed interface SalonError {
    data object NoConnectivity : SalonError
    data class Timeout(val cause: Throwable) : SalonError
    data class Serialization(val cause: Throwable) : SalonError
    data class Unauthorized(val message: String?) : SalonError
    data class Forbidden(val message: String?) : SalonError
    data class NotFound(val message: String?) : SalonError
    data class Conflict(val message: String?) : SalonError
    data class Validation(val message: String?, val fieldErrors: Map<String, String>) : SalonError
    data class ServerError(val code: Int, val message: String?) : SalonError
    data class Unknown(val cause: Throwable) : SalonError

    companion object {
        fun fromHttpStatus(
            code: Int,
            message: String?,
            fieldErrors: Map<String, String> = emptyMap(),
        ): SalonError = when (code) {
            401 -> Unauthorized(message)
            403 -> Forbidden(message)
            404 -> NotFound(message)
            409 -> Conflict(message)
            422 -> Validation(message, fieldErrors)
            in 500..599 -> ServerError(code, message)
            else -> ServerError(code, message)
        }
    }
}

/**
 * Extension to provide message casting to UI.
 */
fun SalonError.asUiMessage(): String = when (this) {
    SalonError.NoConnectivity -> "No internet connection. Please check your network."
    is SalonError.Timeout -> "Connection timed out. Please try again."
    is SalonError.Serialization -> "Error parsing data from server."
    is SalonError.Unauthorized -> message ?: "Session expired. Please log in again."
    is SalonError.Forbidden -> message ?: "You don't have permission to perform this action."
    is SalonError.NotFound -> message ?: "The requested resource was not found."
    is SalonError.Conflict -> message ?: "A conflict occurred with the current state."
    is SalonError.Validation -> message ?: "Please check the entered information."
    is SalonError.ServerError -> message ?: "A server error occurred (Error $code)."
    is SalonError.Unknown -> "An unexpected error occurred."
}

class SalonApiException(val error: SalonError) : Exception("API call failed: $error")
