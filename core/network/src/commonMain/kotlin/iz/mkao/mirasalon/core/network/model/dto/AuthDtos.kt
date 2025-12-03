package iz.mkao.mirasalon.core.network.model.dto

import iz.mkao.mirasalon.core.domain.model.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val role: UserRole = UserRole.USER,
    val avatarUrl: String? = null,
    val address: String? = null,
    val referralCode: String? = null
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val address: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val refreshToken: String? = null
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val gender: String? = null,
    val referralCode: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val gender: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class AvatarUploadResponse(
    val avatarUrl: String
)
