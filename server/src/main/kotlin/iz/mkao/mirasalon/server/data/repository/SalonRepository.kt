package iz.mkao.mirasalon.server.data.repository

import iz.mkao.mirasalon.core.domain.model.Salon
import iz.mkao.mirasalon.core.domain.model.SalonHome
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.SalonDto
import iz.mkao.mirasalon.core.network.model.dto.SalonPaginatedResponseDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateSalonRequest
import iz.mkao.mirasalon.server.data.tables.SalonsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import iz.mkao.mirasalon.core.domain.repository.SalonRepository as CoreSalonRepository

sealed class SalonFetchResult {
    data class Success(val salon: SalonDto) : SalonFetchResult()
    data object NotFound : SalonFetchResult()
}

sealed class SalonUpdateResult {
    data object Success : SalonUpdateResult()
    data object NotFound : SalonUpdateResult()
    data class Failure(val message: String) : SalonUpdateResult()
}

class SalonRepository : CoreSalonRepository {

    override suspend fun getHome(): Outcome<SalonHome> {
        return Outcome.Success(SalonHome(
            categories = emptyList(),
            specialists = emptyList(),
            promotions = emptyList(),
            isLoggedIn = false
        ))
    }

    override suspend fun getSalon(id: String): Outcome<Salon> {
        return when (val result = findById(id)) {
            is SalonFetchResult.Success -> Outcome.Success(result.salon.toDomain())
            is SalonFetchResult.NotFound -> Outcome.Error(Failure.ClientError(404, "Salon not found"))
        }
    }

    fun findById(id: String): SalonFetchResult = transaction {
        SalonsTable.selectAll().where { SalonsTable.id eq id }
            .map { it.toSalonDto() }
            .singleOrNull()?.let { SalonFetchResult.Success(it) } ?: SalonFetchResult.NotFound
    }

    fun findAll(page: Int, pageSize: Int): SalonPaginatedResponseDto = transaction {
        val total = SalonsTable.selectAll().count()
        val items = SalonsTable.selectAll()
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map { it.toSalonDto() }
        
        SalonPaginatedResponseDto(items, total, page, pageSize)
    }

    fun update(id: String, request: UpdateSalonRequest): SalonUpdateResult = transaction {
        val updatedRows = SalonsTable.update({ SalonsTable.id eq id }) {
            request.name?.let { name -> it[SalonsTable.name] = name }
            request.address?.let { address -> it[SalonsTable.address] = address }
            request.imageUrl?.let { imageUrl -> it[SalonsTable.imageUrl] = imageUrl }
            request.phone?.let { phone -> it[SalonsTable.phone] = phone }
            request.openTime?.let { openTime -> it[SalonsTable.openTime] = openTime }
            request.closeTime?.let { closeTime -> it[SalonsTable.closeTime] = closeTime }
            request.timezoneId?.let { timezoneId -> it[SalonsTable.timezoneId] = timezoneId }
        }

        if (updatedRows > 0) SalonUpdateResult.Success
        else SalonUpdateResult.NotFound
    }

    fun getImagePath(id: String): String? = transaction {
        SalonsTable.select(SalonsTable.imageUrl)
            .where { SalonsTable.id eq id }
            .map { it[SalonsTable.imageUrl] }
            .singleOrNull()
    }

    private fun ResultRow.toSalonDto() = SalonDto(
        id = this[SalonsTable.id],
        name = this[SalonsTable.name],
        address = this[SalonsTable.address],
        imageUrl = if (this[SalonsTable.imageUrl] != null) "/v1/api/salon/image/${this[SalonsTable.id]}" else null,
        phone = this[SalonsTable.phone],
        rating = this[SalonsTable.rating] ?: 0.0,
        openTime = this[SalonsTable.openTime],
        closeTime = this[SalonsTable.closeTime],
        timezoneId = this[SalonsTable.timezoneId]
    )

    private fun SalonDto.toDomain() = Salon(
        id = id,
        name = name,
        address = address,
        imageUrl = imageUrl,
        rating = rating,
        openTime = openTime ?: "08:00",
        closeTime = closeTime ?: "20:00",
        timezoneId = timezoneId ?: "UTC"
    )
}
