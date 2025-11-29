package iz.mkao.mirasalon.core.network.result

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.ApiResponse

suspend inline fun <reified T> apiCall(crossinline call: suspend () -> HttpResponse): Outcome<T> {
    return try {
        val response = call()
        val apiResponse = response.body<ApiResponse<T>>()
        if (apiResponse.success) {
            val data = apiResponse.data
            if (data != null) {
                Outcome.Success(data)
            } else if (T::class == Unit::class) {
                Outcome.Success(Unit as T)
            } else {
                Outcome.Error(Failure.ServerError(response.status.value, "API returned success but data was null"))
            }
        } else {
            Outcome.Error(Failure.ServerError(response.status.value, apiResponse.error ?: "API returned failure"))
        }
    } catch (e: RedirectResponseException) {
        Outcome.Error(Failure.ServerError(e.response.status.value, e.message))
    } catch (e: ClientRequestException) {
        if (e.response.status.value == 401 || e.response.status.value == 403) {
            Outcome.Error(Failure.SessionExpired)
        } else {
            Outcome.Error(Failure.ClientError(e.response.status.value, e.message))
        }
    } catch (e: ServerResponseException) {
        Outcome.Error(Failure.ServerError(e.response.status.value, e.message))
    } catch (e: Exception) {
        Outcome.Error(Failure.Unknown)
    }
}
