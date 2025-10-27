package iz.mkao.mirasalon.server.data.repository

import io.micrometer.core.instrument.MeterRegistry
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.AppointmentDto
import iz.mkao.mirasalon.core.network.model.dto.AppointmentStatusDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceItemDto
import iz.mkao.mirasalon.server.data.tables.AppointmentsTable
import iz.mkao.mirasalon.server.data.tables.SalonsTable
import iz.mkao.mirasalon.server.data.tables.ServicesTable
import iz.mkao.mirasalon.server.data.tables.SpecialistsTable
import iz.mkao.mirasalon.server.data.tables.UsersTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

enum class AppointmentStatus {
    CONFIRMED, COMPLETED, CANCELLED;

    companion object {
        fun fromString(value: String): AppointmentStatus? = entries.find { it.name.equals(value, ignoreCase = true) }
    }
}

sealed class BookingResult {
    data class Success(val appointment: AppointmentDto) : BookingResult()
    sealed class Error(val message: String) : BookingResult() {
        data class ScheduleOverlap(val msg: String) : Error(msg)
        data class ServiceUnavailable(val msg: String) : Error(msg)
        data class SpecialistNotFound(val msg: String) : Error(msg)
        data class SpecialistUnqualified(val msg: String) : Error(msg)
        data class SalonMismatch(val msg: String) : Error(msg)
        data class LeadTimeViolation(val msg: String) : Error(msg)
        data class ShiftMismatch(val msg: String) : Error(msg)
        data class SpecialistAbsent(val msg: String) : Error(msg)
        data class PromoError(val msg: String) : Error(msg)
        data class Generic(val msg: String) : Error(msg)
    }
}

sealed class AppointmentUpdateResult {
    data class Success(val appointment: AppointmentDto) : AppointmentUpdateResult()
    data object NotFound : AppointmentUpdateResult()
    data class Error(val message: String) : AppointmentUpdateResult()
}

sealed class CancelResult {
    data object Success : CancelResult()
    data object NotFound : CancelResult()
    data object AlreadyCancelled : CancelResult()
    data object CannotCancelPast : CancelResult()
    data object TooLateToCancel : CancelResult()
    data object Unauthorized : CancelResult()
    data class DatabaseError(val cause: String) : CancelResult()
}

class AppointmentRepository(
    private val outboxRepository: OutboxRepository,
    private val json: Json,
    private val meterRegistry: MeterRegistry
) {

    @Volatile
    private var autoCompletionScope: CoroutineScope? = null

    // --- Routes & Analytics Methods ---

    fun countByStatusInRange(status: AppointmentStatus, start: Long, end: Long): Int = transaction {
        AppointmentsTable.selectAll().where {
            (AppointmentsTable.status eq status.name) and
            (AppointmentsTable.dateTime greaterEq start) and
            (AppointmentsTable.dateTime less end)
        }.count().toInt()
    }

    fun totalRevenue(): Double = transaction {
        AppointmentsTable.selectAll().where {
            (AppointmentsTable.status eq AppointmentStatus.COMPLETED.name) or
            (AppointmentsTable.status eq AppointmentStatus.CONFIRMED.name)
        }.sumOf { it[AppointmentsTable.totalAmount] }
    }

    fun totalRevenueInRange(start: Long, end: Long): Double = transaction {
        AppointmentsTable.selectAll().where {
            ((AppointmentsTable.status eq AppointmentStatus.COMPLETED.name) or
             (AppointmentsTable.status eq AppointmentStatus.CONFIRMED.name)) and
            (AppointmentsTable.dateTime greaterEq start) and
            (AppointmentsTable.dateTime less end)
        }.sumOf { it[AppointmentsTable.totalAmount] }
    }

    fun countAll(): Int = transaction {
        AppointmentsTable.selectAll().count().toInt()
    }

    fun countByStatus(status: AppointmentStatus): Int = transaction {
        AppointmentsTable.selectAll().where { AppointmentsTable.status eq status.name }.count().toInt()
    }

    fun findByDateRange(start: Long, end: Long): List<AppointmentDto> = transaction {
        AppointmentsTable
            .join(SalonsTable, JoinType.INNER, AppointmentsTable.salonId, SalonsTable.id)
            .join(SpecialistsTable, JoinType.INNER, AppointmentsTable.specialistId, SpecialistsTable.id)
            .join(UsersTable, JoinType.INNER, AppointmentsTable.userId, UsersTable.id)
            .selectAll().where {
                (AppointmentsTable.dateTime greaterEq start) and
                (AppointmentsTable.dateTime less end)
            }
            .map { it.toAppointmentDto() }
    }

    fun hasAppointmentBefore(userId: String, timestamp: Long): Boolean = transaction {
        AppointmentsTable.selectAll().where {
            (AppointmentsTable.userId eq userId) and
            (AppointmentsTable.dateTime less timestamp)
        }.any()
    }

    fun findById(id: String): AppointmentDto? = transaction {
        AppointmentsTable
            .join(SalonsTable, JoinType.INNER, AppointmentsTable.salonId, SalonsTable.id)
            .join(SpecialistsTable, JoinType.INNER, AppointmentsTable.specialistId, SpecialistsTable.id)
            .join(UsersTable, JoinType.INNER, AppointmentsTable.userId, UsersTable.id)
            .selectAll().where { AppointmentsTable.id eq id }
            .map { it.toAppointmentDto() }
            .singleOrNull()
    }

    fun findAllPaginated(
        page: Int,
        pageSize: Int,
        status: AppointmentStatus?,
        specialistId: String?,
        query: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null
    ): PagedResponse<AppointmentDto> = transaction {
        val baseQuery = AppointmentsTable
            .join(SalonsTable, JoinType.INNER, AppointmentsTable.salonId, SalonsTable.id)
            .join(SpecialistsTable, JoinType.INNER, AppointmentsTable.specialistId, SpecialistsTable.id)
            .join(UsersTable, JoinType.INNER, AppointmentsTable.userId, UsersTable.id)
            .selectAll()
        status?.let { baseQuery.andWhere { AppointmentsTable.status eq it.name } }
        specialistId?.let { baseQuery.andWhere { AppointmentsTable.specialistId eq it } }

        query?.let { q ->
            val searchTerm = "%${q.lowercase()}%"
            baseQuery.andWhere {
                (AppointmentsTable.id.lowerCase() like searchTerm) or
                (SpecialistsTable.name.lowerCase() like searchTerm) or
                (UsersTable.name.lowerCase() like searchTerm)
            }
        }

        dateFrom?.let { baseQuery.andWhere { AppointmentsTable.dateTime greaterEq it } }
        dateTo?.let { baseQuery.andWhere { AppointmentsTable.dateTime lessEq it } }

        val total = baseQuery.count()
        val items = baseQuery.limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .orderBy(AppointmentsTable.dateTime to SortOrder.DESC)
            .map { it.toAppointmentDto() }

        PagedResponse(items, total, page, pageSize, if (pageSize > 0) ((total + pageSize - 1) / pageSize).toInt() else 0)
    }

    fun findByUserPaginated(userId: String, page: Int, pageSize: Int): PagedResponse<AppointmentDto> = transaction {
        val query = AppointmentsTable
            .join(SalonsTable, JoinType.INNER, AppointmentsTable.salonId, SalonsTable.id)
            .join(SpecialistsTable, JoinType.INNER, AppointmentsTable.specialistId, SpecialistsTable.id)
            .join(UsersTable, JoinType.INNER, AppointmentsTable.userId, UsersTable.id)
            .selectAll().where { AppointmentsTable.userId eq userId }
        
        val total = query.count()
        val items = query.limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .orderBy(AppointmentsTable.dateTime to SortOrder.DESC)
            .map { it.toAppointmentDto() }
        
        PagedResponse(items, total, page, pageSize, (total / pageSize).toInt() + 1)
    }

    fun create(
        userId: String,
        salonId: String,
        specialistId: String,
        dateTimeMillis: Long,
        serviceIds: List<String>,
        promoCode: String?
    ): BookingResult = transaction {
        try {
            val log = LoggerFactory.getLogger(AppointmentRepository::class.java)
            log.info("Creating appointment: user=$userId, salon=$salonId, specialist=$specialistId, services=$serviceIds")

            // Fetch selected services to calculate total and build JSON
            val selectedServices = ServicesTable.selectAll()
                .where { ServicesTable.id inList serviceIds }
                .map {
                    ServiceItemDto(
                        id = it[ServicesTable.id],
                        name = it[ServicesTable.name],
                        price = it[ServicesTable.price],
                        durationMinutes = it[ServicesTable.durationMinutes],
                        discountPercent = 0 // Default for now
                    )
                }

            if (selectedServices.isEmpty()) {
                return@transaction BookingResult.Error.Generic("No valid services found")
            }

            val total = selectedServices.sumOf { it.price }
            val totalDuration = selectedServices.sumOf { it.durationMinutes }
            val servicesJsonStr = json.encodeToString(selectedServices)
            val id = UUID.randomUUID().toString()

            AppointmentsTable.insert {
                it[this.id] = id
                it[this.userId] = userId
                it[this.salonId] = salonId
                it[this.specialistId] = specialistId
                it[this.status] = AppointmentStatus.CONFIRMED.name
                it[this.dateTime] = dateTimeMillis
                it[this.servicesJson] = servicesJsonStr
                it[this.totalAmount] = total
                it[this.subtotalAmount] = total
                it[this.durationMinutes] = totalDuration
                it[this.createdAt] = System.currentTimeMillis()
                it[this.promoCode] = promoCode
            }

            val created = findById(id)
            if (created != null) {
                val event = DomainEvent.BookingCreated(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    actorId = userId,
                    message = "New appointment scheduled",
                    bookingId = id,
                    specialistId = specialistId,
                    startTime = dateTimeMillis,
                    appointmentId = id
                )
                outboxRepository.save(userId, json.encodeToString(event))
                meterRegistry.counter("appointments_booked_total", "salon_id", salonId).increment()
                BookingResult.Success(created)
            } else {
                log.error("Failed to retrieve created booking $id. Join issue?")
                // Debug: Check if parts exist
                val userExists = UsersTable.selectAll().where { UsersTable.id eq userId }.any()
                val salonExists = SalonsTable.selectAll().where { SalonsTable.id eq salonId }.any()
                val specialistExists = SpecialistsTable.selectAll().where { SpecialistsTable.id eq specialistId }.any()
                log.error("Debug info - userExists: $userExists, salonExists: $salonExists, specialistExists: $specialistExists")
                
                BookingResult.Error.Generic("Failed to retrieve created booking after insertion")
            }
        } catch (e: Exception) {
            LoggerFactory.getLogger(AppointmentRepository::class.java).error("Booking failed", e)
            BookingResult.Error.Generic(e.message ?: "Database error")
        }
    }

    fun updateStatus(id: String, status: AppointmentStatus): AppointmentUpdateResult = transaction {
        val updated = AppointmentsTable.update({ AppointmentsTable.id eq id }) {
            it[this.status] = status.name
        }
        if (updated > 0) {
            val appointment = findById(id)
            if (appointment != null) {
                val event = DomainEvent.BookingUpdated(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    actorId = appointment.userId,
                    message = "Appointment status updated to ${status.name}",
                    bookingId = id,
                    status = status.name,
                    appointmentId = id
                )
                outboxRepository.save(appointment.userId, json.encodeToString(event))
                AppointmentUpdateResult.Success(appointment)
            } else AppointmentUpdateResult.NotFound
        } else {
            AppointmentUpdateResult.NotFound
        }
    }

    fun updateReminderEnabled(id: String, userId: String, enabled: Boolean): Result<Unit> = transaction {
        try {
            val updated = AppointmentsTable.update({ (AppointmentsTable.id eq id) and (AppointmentsTable.userId eq userId) }) {
                it[reminderEnabled] = enabled
            }
            if (updated > 0) Result.success(Unit)
            else Result.failure(IllegalArgumentException("Appointment not found or unauthorized"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cancel(id: String, userId: String, isAdmin: Boolean): CancelResult = transaction {
        val appointment = AppointmentsTable.selectAll().where { AppointmentsTable.id eq id }.singleOrNull()
            ?: return@transaction CancelResult.NotFound

        if (!isAdmin) {
            return@transaction CancelResult.Unauthorized
        }

        if (appointment[AppointmentsTable.status] == "CANCELLED") {
            return@transaction CancelResult.AlreadyCancelled
        }

        AppointmentsTable.update({ AppointmentsTable.id eq id }) {
            it[status] = AppointmentStatus.CANCELLED.name
        }
        
        val event = DomainEvent.BookingUpdated(
            eventId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            actorId = userId,
            message = "Appointment cancelled",
            bookingId = id,
            status = "CANCELLED",
            appointmentId = id
        )
        outboxRepository.save(userId, json.encodeToString(event))
        
        CancelResult.Success
    }

    fun delete(id: String): Boolean = transaction {
        AppointmentsTable.deleteWhere { AppointmentsTable.id eq id } > 0
    }

    fun completePastDayBookings(defaultZoneId: ZoneId = ZoneId.systemDefault()): Int {
        val salon = transaction { SalonsTable.selectAll().limit(1).singleOrNull() }
        val zoneId = salon?.get(SalonsTable.timezoneId)?.let { try { ZoneId.of(it) } catch (e: Exception) { defaultZoneId } } ?: defaultZoneId
        
        val startOfTodayMillis = LocalDate.now(zoneId)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        return transaction {
            AppointmentsTable.update({
                (AppointmentsTable.status eq AppointmentStatus.CONFIRMED.name) and (AppointmentsTable.dateTime less startOfTodayMillis)
            }) {
                it[status] = AppointmentStatus.COMPLETED.name
            }
        }
    }

    fun startAutoCompletionScheduler(
        defaultZoneId: ZoneId = ZoneId.systemDefault(),
        intervalMillis: Long = AUTO_COMPLETE_INTERVAL_MILLIS
    ) {
        if (autoCompletionScope != null) return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        autoCompletionScope = scope
        scope.launch {
            while (isActive) {
                runCatching {
                    val completed = completePastDayBookings(defaultZoneId)
                    if (completed > 0) {
                        LoggerFactory.getLogger(AppointmentRepository::class.java)
                            .info("Auto-completed {} booking(s) whose scheduled day passed", completed)
                    }
                }
                delay(intervalMillis.milliseconds)
            }
        }
    }

    fun stopAutoCompletionScheduler() {
        autoCompletionScope?.cancel()
        autoCompletionScope = null
    }

    private fun ResultRow.toAppointmentDto(): AppointmentDto {
        val log = LoggerFactory.getLogger("AppointmentRepo")
        val servicesStr = this[AppointmentsTable.servicesJson]
        val services = try {
            json.decodeFromString<List<ServiceItemDto>>(servicesStr)
        } catch (e: Exception) {
            log.warn("Failed to decode services JSON for appointment {}: {}", this[AppointmentsTable.id], servicesStr)
            emptyList()
        }

        val isReviewedVal = this[AppointmentsTable.isReviewed]
        log.debug("Mapping appointment {} to DTO, isReviewed: {}", this[AppointmentsTable.id], isReviewedVal)

        return AppointmentDto(
            id = this[AppointmentsTable.id],
            userId = this[AppointmentsTable.userId],
            userName = this.getOrNull(UsersTable.name),
            salonId = this[AppointmentsTable.salonId],
            salonName = this[SalonsTable.name],
            salonAddress = this[SalonsTable.address],
            salonImageUrl = this[SalonsTable.imageUrl],
            specialistId = this[AppointmentsTable.specialistId],
            specialistName = this[SpecialistsTable.name],
            specialistAvatarUrl = this[SpecialistsTable.imageUrl],
            status = try {
                AppointmentStatusDto.valueOf(this[AppointmentsTable.status])
            } catch (e: Exception) {
                log.warn("Unknown appointment status '{}' for {}, defaulting to CONFIRMED", this[AppointmentsTable.status], this[AppointmentsTable.id])
                AppointmentStatusDto.CONFIRMED
            },
            dateTime = this[AppointmentsTable.dateTime],
            durationMinutes = this[AppointmentsTable.durationMinutes],
            services = services,
            subtotalAmount = this[AppointmentsTable.subtotalAmount],
            taxRatePercent = this[SalonsTable.taxRatePercent],
            taxAmount = this[AppointmentsTable.taxAmount],
            discountAmount = this[AppointmentsTable.discountAmount],
            promoCode = this[AppointmentsTable.promoCode],
            totalAmount = this[AppointmentsTable.totalAmount],
            reminderEnabled = this[AppointmentsTable.reminderEnabled],
            isReviewed = isReviewedVal
        )
    }

    private companion object {
        const val AUTO_COMPLETE_INTERVAL_MILLIS = 15 * 60 * 1000L
    }
}
