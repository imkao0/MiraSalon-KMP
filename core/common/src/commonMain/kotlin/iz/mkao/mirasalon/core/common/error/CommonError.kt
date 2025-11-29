package iz.mkao.mirasalon.core.common.error

sealed class CommonError(val message: String) {
    data class Network(val msg: String) : CommonError(msg)
    data class Server(val msg: String) : CommonError(msg)
    data class Unknown(val msg: String = "An unknown error occurred") : CommonError(msg)
}

fun Throwable.toCommonError(): CommonError = CommonError.Unknown(this.message ?: "An unknown error occurred")
