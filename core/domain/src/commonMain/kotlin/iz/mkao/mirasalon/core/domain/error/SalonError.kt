package iz.mkao.mirasalon.core.domain.error

sealed interface SalonError {
    data object NoConnectivity : SalonError
    data class Timeout(val cause: Throwable) : SalonError
    data class Serialization(val cause: Throwable) : SalonError
    data class Unauthorized(val message: String?) : SalonError
    data class Forbidden(val message: String?) : SalonError
    data class NotFound(val message: String?) : SalonError
    data class Conflict(val message: String?) : SalonError
    data class Validation(val message: String?, val fieldErrors: Map<String, String> = emptyMap()) : SalonError
    data class ServerError(val code: Int, val message: String?) : SalonError
    data class Unknown(val cause: Throwable) : SalonError
}
