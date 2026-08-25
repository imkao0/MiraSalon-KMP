package iz.mkao.mirasalon.core.network.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.SalonApiConfig
import iz.mkao.mirasalon.core.network.model.SalonErrorResponse
import iz.mkao.mirasalon.core.network.model.dto.RefreshTokenRequest
import iz.mkao.mirasalon.core.network.model.dto.RefreshTokenResponse
import iz.mkao.mirasalon.core.network.result.SalonApiException
import iz.mkao.mirasalon.core.network.result.SalonError
import iz.mkao.mirasalon.core.network.util.NetworkJson
import iz.mkao.mirasalon.core.network.util.SalonNetworkLogger
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

internal object SalonNetworkClientFactory {

    fun create(
        config: SalonApiConfig,
        tokenProvider: SalonTokenProvider,
        engine: HttpClientEngine = providePlatformHttpClientEngine(),
    ): HttpClient {
        val refreshClient = createRefreshClient(config)

        return HttpClient(engine) {
            expectSuccess = true

            install(ContentNegotiation) {
                json(NetworkJson)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = config.requestTimeoutMillis
                connectTimeoutMillis = config.connectTimeoutMillis
                socketTimeoutMillis = config.socketTimeoutMillis
            }

            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = config.maxRetries)
                exponentialDelay()
            }

            install(WebSockets) {
                pingIntervalMillis = 20_000
                // Explicitly disable extensions to avoid handshake failures on iOS (Darwin engine)
                // when connecting to Ktor servers over non-TLS ws://
                extensions { }
            }

            if (config.enableLogging) {
                install(Logging) {
                    logger = SalonNetworkLogger
                    level = LogLevel.ALL
                    sanitizeHeader { header -> header == "Authorization" }
                }
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val access = tokenProvider.accessToken() ?: return@loadTokens null
                        val refresh = tokenProvider.refreshToken().orEmpty()
                        BearerTokens(access, refresh)
                    }

                    refreshTokens {
                        val currentRefreshToken = tokenProvider.refreshToken()
                        if (currentRefreshToken.isNullOrBlank()) {
                            tokenProvider.onAuthenticationExpired()
                            return@refreshTokens null
                        }

                        val refreshed = runCatching {
                            refreshClient.post(config.baseUrl.removeSuffix("/") + SalonEndpoints.REFRESH_TOKEN) {
                                contentType(ContentType.Application.Json)
                                setBody(RefreshTokenRequest(currentRefreshToken))
                            }.body<ApiResponse<RefreshTokenResponse>>()
                        }.getOrNull()

                        val tokens = refreshed?.takeIf { it.success }?.data
                        if (tokens == null) {
                            tokenProvider.onAuthenticationExpired()
                            return@refreshTokens null
                        }

                        tokenProvider.onTokensRefreshed(tokens.accessToken, tokens.refreshToken)
                        BearerTokens(tokens.accessToken, tokens.refreshToken)
                    }

                    sendWithoutRequest { request ->
                        !request.url.toString().contains(SalonEndpoints.PATH_PREFIX_AUTH)
                    }
                }
            }

            HttpResponseValidator {
                handleResponseExceptionWithRequest { cause, _ ->
                    val error = when (cause) {
                        is HttpRequestTimeoutException,
                        is ConnectTimeoutException,
                        is SocketTimeoutException -> SalonError.Timeout(cause)
                        is SerializationException -> SalonError.DataParsing(cause)
                        is IOException -> SalonError.NoConnectivity
                        is SalonApiException -> return@handleResponseExceptionWithRequest
                        else -> SalonError.Unknown(cause)
                    }
                    throw SalonApiException(error)
                }

                validateResponse { response ->
                    val status = response.status
                    // Allow 2xx Success and 101 Switching Protocols (for WebSockets)
                    if (!status.isSuccess() && status.value != 101) {
                        val errorBody = runCatching { response.body<SalonErrorResponse>() }.getOrNull()
                        val message = errorBody?.message ?: runCatching { response.bodyAsText() }.getOrNull()
                        val error = SalonError.fromHttpStatus(
                            code = status.value,
                            message = message,
                            fieldErrors = errorBody?.errors.orEmpty(),
                        )
                        throw SalonApiException(error)
                    }
                }
            }

            defaultRequest {
                url.takeFrom(config.baseUrl)
            }
        }
    }

    private fun createRefreshClient(config: SalonApiConfig): HttpClient = HttpClient(providePlatformHttpClientEngine()) {
        expectSuccess = false
        install(ContentNegotiation) { json(NetworkJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMillis
            connectTimeoutMillis = config.connectTimeoutMillis
        }
    }
}
