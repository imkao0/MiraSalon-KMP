package iz.mkao.mirasalon.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.basic
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.util.JwtConfig
import iz.mkao.mirasalon.server.util.ServerEnvironment
import iz.mkao.mirasalon.server.util.TokenVersionCache
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import kotlin.jvm.java

private val authLog = LoggerFactory.getLogger("Auth")

fun Application.configureAuthentication(jwtConfig: JwtConfig) {
    val userRepository: UserRepository by inject(UserRepository::class.java)

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            verifier(jwtConfig.verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                authLog.info("[Auth] Validating token for user: $userId")

                val tokenVersion = if (credential.payload.getClaim("tokenVersion").isNull) 1
                                   else credential.payload.getClaim("tokenVersion").asInt() ?: 1

                if (userId.isNullOrEmpty()) {
                    authLog.error("[Auth] Missing userId in token")
                    null
                } else {
                    val cachedVersion = TokenVersionCache.get(userId)

                    if (cachedVersion != null) {
                        if (cachedVersion == tokenVersion) {
                            JWTPrincipal(credential.payload)
                        } else {
                            authLog.error("[Auth] Token version mismatch for $userId: cached=$cachedVersion, token=$tokenVersion")
                            null
                        }
                    } else {
                        val user = userRepository.findById(userId)
                        if (user != null) {
                            TokenVersionCache.put(userId, user.tokenVersion)
                            if (user.tokenVersion == tokenVersion) {
                                JWTPrincipal(credential.payload)
                            } else {
                                authLog.error("[Auth] Token version mismatch for $userId: db=${user.tokenVersion}, token=$tokenVersion")
                                null
                            }
                        } else {
                            authLog.error("[Auth] User not found in DB: $userId")
                            null
                        }
                    }
                }
            }
        }

        basic("metrics-auth") {
            realm = "Metrics"
            validate { credentials ->
                val expectedUser = ServerEnvironment.orDefault("METRICS_USERNAME", "metrics")
                val expectedPassword = ServerEnvironment.secret("METRICS_PASSWORD")
                val userOk = MessageDigest.isEqual(
                    credentials.name.toByteArray(), expectedUser.toByteArray())
                val passOk = MessageDigest.isEqual(
                    credentials.password.toByteArray(), expectedPassword.toByteArray())
                if (userOk && passOk) UserIdPrincipal(credentials.name) else null
            }
        }
    }
}
