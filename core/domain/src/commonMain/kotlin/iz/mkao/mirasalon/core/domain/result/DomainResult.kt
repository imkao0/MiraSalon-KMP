package iz.mkao.mirasalon.core.domain.result

import iz.mkao.mirasalon.core.domain.error.SalonError

sealed class DomainResult<out T> {
    data class Success<out T>(val data: T) : DomainResult<T>()
    data class Error(val error: SalonError) : DomainResult<Nothing>()
}
