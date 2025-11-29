package iz.mkao.mirasalon.core.network.util

import io.ktor.client.statement.*
import iz.mkao.mirasalon.core.common.result.NetworkResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import iz.mkao.mirasalon.core.network.result.safeApiCall as sharedSafeApiCall

suspend inline fun <reified T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    crossinline block: suspend () -> HttpResponse
): NetworkResult<T> = withContext(dispatcher) {
    try {
        val result = sharedSafeApiCall<T> { block() }
        when (result) {
            is iz.mkao.mirasalon.core.network.result.NetworkResult.Success -> NetworkResult.Success(result.data)
            is iz.mkao.mirasalon.core.network.result.NetworkResult.Error -> NetworkResult.Error(Exception(result.error.message))
        }
    } catch (e: Exception) {
        NetworkResult.Error(e)
    }
}

suspend inline fun safeEmptyApiCall(
    dispatcher: CoroutineDispatcher,
    crossinline block: suspend () -> HttpResponse
): NetworkResult<Unit> = safeApiCall<Unit>(dispatcher, block)
