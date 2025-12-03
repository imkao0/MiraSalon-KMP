package iz.mkao.mirasalon.core.common.error

sealed class NetworkException(message: String) : Exception(message) {
    class NoConnection(message: String = "No internet connection") : NetworkException(message)
    class Timeout(message: String = "Request timed out") : NetworkException(message)
    class ServerError(val code: Int, message: String) : NetworkException(message)
    class Unknown(message: String = "Unknown error") : NetworkException(message)
}
