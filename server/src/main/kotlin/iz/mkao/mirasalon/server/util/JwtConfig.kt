package iz.mkao.mirasalon.server.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import iz.mkao.mirasalon.core.domain.model.UserRole
import java.util.Date

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expiration: Long
) {
    val algorithm: Algorithm = Algorithm.HMAC256(secret)

    val verifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateToken(userId: String, email: String, role: UserRole, tokenVersion: Int): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("role", role.name)
            .withClaim("tokenVersion", tokenVersion)
            .withExpiresAt(Date(System.currentTimeMillis() + expiration))
            .sign(algorithm)

    fun generateRefreshToken(userId: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("isRefreshToken", true)
            .withExpiresAt(Date(System.currentTimeMillis() + expiration * 30))
            .sign(algorithm)
}
