package iz.mkao.mirasalon.server.error

import io.ktor.http.*

sealed class DomainException(
    override val message: String,
    val status: HttpStatusCode = HttpStatusCode.BadRequest
) : Exception(message)

class EmailAlreadyExistsException(msg: String = "Email already registered") : DomainException(msg, HttpStatusCode.Conflict)
class ScheduleOverlapException(msg: String = "Time slot already booked") : DomainException(msg, HttpStatusCode.Conflict)
class ResourceNotFoundException(msg: String = "Resource not found") : DomainException(msg, HttpStatusCode.NotFound)
class UnauthorizedException(msg: String = "Unauthorized access") : DomainException(msg, HttpStatusCode.Unauthorized)
class ForbiddenException(msg: String = "Access denied") : DomainException(msg, HttpStatusCode.Forbidden)
class ServiceUnavailableException(msg: String = "Service no longer available") : DomainException(msg, HttpStatusCode.Gone)
class InsufficientStockException(msg: String = "Insufficient stock") : DomainException(msg, HttpStatusCode.Conflict)

// New exceptions
class InvalidCredentialsException(msg: String = "Invalid email or password") : DomainException(msg, HttpStatusCode.Unauthorized)
class PromoExpiredException(msg: String = "Promotion has expired") : DomainException(msg, HttpStatusCode.BadRequest)
class PromoUsageLimitExceededException(msg: String = "Promotion usage limit reached") : DomainException(msg, HttpStatusCode.Conflict)
class ValidationException(msg: String, val errors: List<String> = emptyList()) : DomainException(msg, HttpStatusCode.UnprocessableEntity)
