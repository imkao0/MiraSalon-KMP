package iz.mkao.mirasalon.core.network.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import iz.mkao.mirasalon.core.network.model.SalonApiConfig
import iz.mkao.mirasalon.core.network.result.SalonApiException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SalonNetworkClientTest {

    private val config = SalonApiConfig(
        baseUrl = "http://test.com/",
        webSocketUrl = "ws://test.com/",
        enableLogging = false
    )

    private class FakeTokenProvider(
        var accessToken: String? = "initial_token",
        var refreshToken: String? = "refresh_token"
    ) : SalonTokenProvider {
        override suspend fun accessToken(): String? = accessToken
        override suspend fun refreshToken(): String? = refreshToken
        override suspend fun userId(): String? = "user_1"
        override suspend fun userName(): String? = null
        override suspend fun userAddress(): String? = null
        override suspend fun userAvatarUrl(): String? = null
        override suspend fun onTokensRefreshed(
            accessToken: String,
            refreshToken: String,
            userId: String?,
            userName: String?,
            userAvatarUrl: String?,
            firstName: String?,
            lastName: String?,
            phone: String?,
            address: String?,
            gender: String?
        ) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
        }
        override suspend fun onAuthenticationExpired() {
            accessToken = null
            refreshToken = null
        }
    }

    @Test
    fun client_adds_auth_header_automatically() = runTest {
        val mockEngine = MockEngine { request ->
            val authHeader = request.headers["Authorization"]
            if (authHeader == "Bearer initial_token") {
                respond("OK")
            } else {
                respondError(HttpStatusCode.Unauthorized)
            }
        }

        val client = SalonNetworkClientFactory.create(config, FakeTokenProvider(), mockEngine)
        val response = client.get("test")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun client_maps_500_to_SalonApiException() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "{\"message\": \"Server Error\"}",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf("Content-Type", "application/json")
            )
        }

        val client = SalonNetworkClientFactory.create(config, FakeTokenProvider(), mockEngine)

        assertFailsWith<SalonApiException> {
            client.get("test")
        }
    }
}
