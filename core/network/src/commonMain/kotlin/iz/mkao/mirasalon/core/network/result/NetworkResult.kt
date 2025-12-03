package iz.mkao.mirasalon.core.network.result

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import iz.mkao.mirasalon.core.network.model.ApiResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val error: NetworkError) : NetworkResult<Nothing>()
}

sealed class NetworkError(val message: String) {
    class NoConnection(message: String = "No internet connection") : NetworkError(message)
    class Timeout(message: String = "Request timed out") : NetworkError(message)
    class HttpError(val code: Int, message: String) : NetworkError(message)
    class Unknown(message: String = "Unknown error") : NetworkError(message)
}

suspend inline fun <reified T> safeApiCall(crossinline block: suspend () -> HttpResponse): NetworkResult<T> {
    return try {
        val response = block()
        if (T::class == Unit::class) {
            return NetworkResult.Success(Unit as T)
        }
        val apiResponse = response.body<ApiResponse<T>>()
        if (apiResponse.success) {
            val data = apiResponse.data
            if (data != null) {
                NetworkResult.Success(data)
            } else if (null is T) {
                NetworkResult.Success(null as T)
            } else {
                NetworkResult.Error(NetworkError.Unknown("API returned success but data was null for non-nullable type"))
            }
        } else {
            NetworkResult.Error(NetworkError.Unknown(apiResponse.error ?: "API returned failure without error message"))
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        NetworkResult.Error(e.toNetworkError())
    }
}

fun Throwable.toNetworkError(): NetworkError = when (this) {
    is SalonApiException -> when (error) {
        is SalonError.NoConnectivity -> NetworkError.NoConnection(error.asUiMessage())
        is SalonError.Timeout -> NetworkError.Timeout(error.asUiMessage())
        is SalonError.Unauthorized -> NetworkError.HttpError(401, error.asUiMessage())
        is SalonError.Forbidden -> NetworkError.HttpError(403, error.asUiMessage())
        is SalonError.NotFound -> NetworkError.HttpError(404, error.asUiMessage())
        is SalonError.Conflict -> NetworkError.HttpError(409, error.asUiMessage())
        is SalonError.Validation -> NetworkError.HttpError(422, error.asUiMessage())
        is SalonError.ServerError -> NetworkError.HttpError(error.code, error.asUiMessage())
        is SalonError.Serialization -> NetworkError.Unknown(error.asUiMessage())
        is SalonError.Unknown -> NetworkError.Unknown(error.asUiMessage())
    }
    is kotlinx.io.IOException -> NetworkError.NoConnection()
    is ClientRequestException -> NetworkError.HttpError(response.status.value, message)
    is ServerResponseException -> NetworkError.HttpError(response.status.value, message)
    is RedirectResponseException -> NetworkError.HttpError(response.status.value, message)
    is SerializationException -> NetworkError.Unknown("Serialization error: ${this.message}")
    else -> NetworkError.Unknown(this.message ?: "Unknown error")
}
