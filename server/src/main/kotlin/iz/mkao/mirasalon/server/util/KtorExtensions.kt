package iz.mkao.mirasalon.server.util

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.server.error.DomainException
import iz.mkao.mirasalon.server.error.ForbiddenException
import iz.mkao.mirasalon.server.error.ValidationException

fun ApplicationCall.getUserId(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

fun ApplicationCall.getUserRole(): UserRole? =
    principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()?.let { UserRole.fromString(it) }

fun ApplicationCall.isAdmin(): Boolean = getUserRole() == UserRole.ADMIN

fun ApplicationCall.isSpecialist(): Boolean = getUserRole() == UserRole.SPECIALIST

fun ApplicationCall.ensureAdmin() {
    if (!isAdmin()) {
        throw ForbiddenException("Access denied: Admin role required")
    }
}

fun validate(block: ValidationScope.() -> Unit) {
    val scope = ValidationScope()
    scope.block()
    if (scope.hasErrors()) {
        throw ValidationException("Validation failed", scope.errors)
    }
}

class ValidationScope {
    val errors = mutableMapOf<String, MutableList<String>>()

    fun addError(message: String) {
        addError("general", message)
    }

    fun addError(field: String, message: String) {
        errors.getOrPut(field) { mutableListOf() }.add(message)
    }

    fun hasErrors() = errors.isNotEmpty()

    fun requireNotBlank(field: String, value: String?) {
        if (value.isNullOrBlank()) {
            addError(field, "$field is required")
        }
    }

    fun requireEmail(field: String, value: String?) {
        if (value != null && !value.contains("@")) {
            addError(field, "Invalid email format")
        }
    }

    fun requireMinLength(field: String, value: String?, min: Int) {
        if (value != null && value.length < min) {
            addError(field, "$field must be at least $min characters")
        }
    }

    fun requireMaxLength(field: String, value: String?, max: Int) {
        if (value != null && value.length > max) {
            addError(field, "$field must be at most $max characters")
        }
    }

    fun requireOneOf(field: String, value: String?, options: List<String>) {
        if (value != null && value !in options) {
            addError(field, "$field must be one of: ${options.joinToString(", ")}")
        }
    }

    fun requireBoolean(field: String, value: Any?) {
        if (value != null && value !is Boolean) {
            addError(field, "$field must be a boolean")
        }
    }

    fun requirePositive(field: String, value: Number?) {
        if (value != null && value.toDouble() <= 0) {
            addError(field, "$field must be positive")
        }
    }

    fun requireNonNegative(field: String, value: Number?) {
        if (value != null && value.toDouble() < 0) {
            addError(field, "$field must be non-negative")
        }
    }

    fun requireInRange(field: String, value: Number?, min: Double, max: Double) {
        if (value != null) {
            val v = value.toDouble()
            if (v < min || v > max) {
                addError(field, "$field must be between $min and $max")
            }
        }
    }
}
