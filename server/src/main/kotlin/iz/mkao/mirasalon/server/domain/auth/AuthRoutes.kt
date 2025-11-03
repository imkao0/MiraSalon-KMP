package iz.mkao.mirasalon.server.domain.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.AuthResponse
import iz.mkao.mirasalon.core.network.model.dto.LoginRequest
import iz.mkao.mirasalon.core.network.model.dto.RefreshTokenRequest
import iz.mkao.mirasalon.core.network.model.dto.RefreshTokenResponse
import iz.mkao.mirasalon.core.network.model.dto.RegisterRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateProfileRequest
import iz.mkao.mirasalon.server.data.repository.DeleteAccountResult
import iz.mkao.mirasalon.server.data.repository.RefreshToken
import iz.mkao.mirasalon.server.data.repository.RefreshTokenRepository
import iz.mkao.mirasalon.server.data.repository.RegisterResult
import iz.mkao.mirasalon.server.data.repository.UpdateProfileResult
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.service.StreamSyncService
import iz.mkao.mirasalon.server.util.JwtConfig
import iz.mkao.mirasalon.server.util.ensureAdmin
import iz.mkao.mirasalon.server.util.validate
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AuthRoutes")

fun Route.authRoutes(
    userRepository: UserRepository,
    jwtConfig: JwtConfig,
    streamSyncService: StreamSyncService? = null,
    refreshTokenRepository: RefreshTokenRepository
) {

    // ---- Registration ----
    post("/register") {
        val request = call.receive<RegisterRequest>()
        validate {
            requireNotBlank("name", request.name)
            requireMaxLength("name", request.name, 100)
            requireNotBlank("email", request.email)
            requireEmail("email", request.email)
            requireNotBlank("password", request.password)
            requireMinLength("password", request.password, 8)
            requireMaxLength("password", request.password, 128)
            requireOneOf("role", request.role.name, listOf("USER", "ADMIN", "SPECIALIST"))
        }

        // VULNERABILITY FIX: Ignore requested role for public registration.
        // Always register as USER. Admin/Specialist accounts must be created by an Admin.
        val safeRequest = request.copy(role = UserRole.USER)

        val result = userRepository.register(safeRequest)

        when (result) {
            is RegisterResult.Success -> {
                val authResponse = result.authResponse
        
                try {
                    streamSyncService?.syncUser(
                        userId = authResponse.userId,
                        name = authResponse.name,
                        role = authResponse.role,
                        avatarUrl = authResponse.avatarUrl
                    )
                } catch (e: Exception) {
                    log.error("Failed to sync user {} to Stream on registration: {}", authResponse.userId, e.message)
                }

                val token = jwtConfig.generateToken(
                    authResponse.userId,
                    authResponse.email,
                    authResponse.role,
                    1
                )
                val refreshToken = jwtConfig.generateRefreshToken(authResponse.userId)
                refreshTokenRepository.insert(
                    RefreshToken(
                        token = refreshToken,
                        userId = authResponse.userId,
                        expiresAt = System.currentTimeMillis() + (jwtConfig.expiration * 30),
                        revoked = false,
                        createdAt = System.currentTimeMillis()
                    )
                )

                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse(success = true, data = authResponse.copy(token = token, refreshToken = refreshToken))
                )
            }
            is RegisterResult.Failure -> {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = result.message)
                )
            }
        }
    }

    // ---- Login ----
    post("/login") {
        val request = call.receive<LoginRequest>()
        validate {
            requireNotBlank("email", request.email)
            requireEmail("email", request.email)
            requireNotBlank("password", request.password)
        }

        val user = userRepository.findByEmail(request.email)
            ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, error = "Invalid credentials")
            )

        if (!userRepository.verifyPassword(request.password, user.passwordHash)) {
            return@post call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, error = "Invalid credentials")
            )
        }

        if (!user.isActive) {
            return@post call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<Unit>(success = false, error = "Account disabled by administrator")
            )
        }

        try {
            streamSyncService?.syncUser(
                userId = user.id,
                name = user.name,
                role = user.role,
                avatarUrl = user.avatarUrl
            )
        } catch (e: Exception) {
            log.error("Failed to sync user {} to Stream on login: {}", user.id, e.message)
        }

        val token = jwtConfig.generateToken(user.id, user.email, user.role, user.tokenVersion)
        val refreshToken = jwtConfig.generateRefreshToken(user.id)
        refreshTokenRepository.insert(
            RefreshToken(
                token = refreshToken,
                userId = user.id,
                expiresAt = System.currentTimeMillis() + (jwtConfig.expiration * 30),
                revoked = false,
                createdAt = System.currentTimeMillis()
            )
        )

        call.respond(
            HttpStatusCode.OK,
            ApiResponse(
                success = true,
                data = AuthResponse(
                    token = token,
                    refreshToken = refreshToken,
                    userId = user.id,
                    email = user.email,
                    name = user.name,
                    role = user.role,
                    avatarUrl = user.avatarUrl,
                    address = user.address,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    phone = user.phone,
                    gender = user.gender
                )
            )
        )
    }

    // ---- Protected Routes ----
    authenticate("auth-jwt") {

        // Get Profile
        get("/profile") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asString()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Invalid token")
                )

            val user = userRepository.findById(userId)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(success = false, error = "User not found")
                )

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = AuthResponse(
                        token = "", // Not needed for profile fetch
                        userId = user.id,
                        email = user.email,
                        name = user.name,
                        role = user.role,
                        avatarUrl = user.avatarUrl,
                        address = user.address,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        phone = user.phone,
                        gender = user.gender
                    )
                )
            )
        }

        // Update Profile
        put("/profile") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asString()
                ?: return@put call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Invalid token")
                )

            val request = call.receive<UpdateProfileRequest>()
            validate {
                request.phone?.let { phone ->
                    if (!phone.matches(Regex("^\\+?[0-9\\s()-]{10,20}$"))) {
                        addError("Invalid phone number format")
                    }
                }
            }

            val result = userRepository.updateProfile(
                userId = userId,
                firstName = request.firstName,
                lastName = request.lastName,
                phone = request.phone,
                address = request.address,
                gender = request.gender,
                avatarUrl = request.avatarUrl
            )

            when (result) {
                is UpdateProfileResult.Success -> {
                    // Sync updated profile to Stream (Non-blocking)
                    try {
                        userRepository.findById(userId)?.let { user ->
                            streamSyncService?.syncUser(
                                userId = user.id,
                                name = user.name,
                                role = user.role,
                                avatarUrl = user.avatarUrl
                            )
                        }
                    } catch (e: Exception) {
                        log.error("Failed to sync user {} to Stream on profile update: {}", userId, e.message)
                    }
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Profile updated"))
                }
                is UpdateProfileResult.NotFound -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "User not found"))
                }
                is UpdateProfileResult.Failure -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }

        // Delete Account
        delete("/profile") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asString()
                ?: return@delete call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Invalid token")
                )

            val result = userRepository.deleteAccount(userId)
            when (result) {
                is DeleteAccountResult.Success -> {
                    refreshTokenRepository.revokeAllForUser(userId)
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Account deleted"))
                }
                is DeleteAccountResult.NotFound -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "User not found"))
                }
                is DeleteAccountResult.Failure -> {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }

        post("/admin/register-staff") {
            call.ensureAdmin()
            val request = call.receive<RegisterRequest>()
            validate {
                requireNotBlank("name", request.name)
                requireNotBlank("email", request.email)
                requireEmail("email", request.email)
                requireNotBlank("password", request.password)
                requireMinLength("password", request.password, 8)
            }

    
            val result = userRepository.register(request)

            when (result) {
                is RegisterResult.Success -> {
                    val authResponse = result.authResponse
            
                    try {
                        streamSyncService?.syncUser(
                            userId = authResponse.userId,
                            name = authResponse.name,
                            role = authResponse.role,
                            avatarUrl = authResponse.avatarUrl
                        )
                    } catch (e: Exception) {
                        log.error("Failed to sync user {} to Stream on staff registration: {}", authResponse.userId, e.message)
                    }
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = authResponse))
                }
                is RegisterResult.Failure -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }
    }

    post("/refresh") {
        val request = call.receive<RefreshTokenRequest>()
        val storedToken = refreshTokenRepository.findByToken(request.refreshToken)

        if (storedToken == null || storedToken.revoked || storedToken.expiresAt < System.currentTimeMillis()) {
            return@post call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, error = "Invalid or expired refresh token")
            )
        }

        val user = userRepository.findById(storedToken.userId)
            ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, error = "User not found")
            )

        val newAccessToken = jwtConfig.generateToken(user.id, user.email, user.role, user.tokenVersion)
        val newRefreshToken = jwtConfig.generateRefreshToken(user.id)

        // Revoke old token and insert new one (or rotate)
        refreshTokenRepository.revoke(request.refreshToken)
        refreshTokenRepository.insert(
            RefreshToken(
                token = newRefreshToken,
                userId = user.id,
                expiresAt = System.currentTimeMillis() + (jwtConfig.expiration * 30),
                revoked = false,
                createdAt = System.currentTimeMillis()
            )
        )

        call.respond(
            HttpStatusCode.OK,
            ApiResponse(
                success = true,
                data = RefreshTokenResponse(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken
                )
            )
        )
    }
}
