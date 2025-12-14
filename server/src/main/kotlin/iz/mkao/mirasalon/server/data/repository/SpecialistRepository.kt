package iz.mkao.mirasalon.server.data.repository

import io.micrometer.core.instrument.MeterRegistry
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.model.SpecialistReview
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.CreateSpecialistRequestDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistPerformanceDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistReviewDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistShiftDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateSpecialistRequestDto
import iz.mkao.mirasalon.server.data.tables.AppointmentsTable
import iz.mkao.mirasalon.server.data.tables.ReviewsTable
import iz.mkao.mirasalon.server.data.tables.ServicesTable
import iz.mkao.mirasalon.server.data.tables.SpecialistServicesTable
import iz.mkao.mirasalon.server.data.tables.SpecialistShiftsTable
import iz.mkao.mirasalon.server.data.tables.SpecialistsTable
import iz.mkao.mirasalon.server.data.tables.UsersTable
import iz.mkao.mirasalon.server.service.StreamSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
import java.util.UUID
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository as CoreSpecialistRepository

sealed class SpecialistFetchResult {
    data class Success(val specialist: SpecialistDto) : SpecialistFetchResult()
    data object NotFound : SpecialistFetchResult()
}

sealed class SpecialistCreateResult {
    data class Success(val specialist: SpecialistDto) : SpecialistCreateResult()
    data class Failure(val message: String) : SpecialistCreateResult()
}

sealed class SpecialistUpdateResult {
    data object Success : SpecialistUpdateResult()
    data object NotFound : SpecialistUpdateResult()
    data class Failure(val message: String) : SpecialistUpdateResult()
}

sealed class SpecialistStatusUpdateResult {
    data object Success : SpecialistStatusUpdateResult()
    data object NotFound : SpecialistStatusUpdateResult()
    data class Failure(val message: String) : SpecialistStatusUpdateResult()
}

class SpecialistRepository(
    private val outboxRepository: OutboxRepository,
    private val streamSyncService: StreamSyncService,
    private val json: Json,
    private val meterRegistry: MeterRegistry,
    private val repositoryScope: CoroutineScope
) : CoreSpecialistRepository {

    fun registerMetrics() {
        meterRegistry.gauge("specialists_active_count", this) {
            transaction {
                try {
                    SpecialistsTable.selectAll().where { (SpecialistsTable.isActive eq true) and (SpecialistsTable.isDeleted eq false) }.count().toDouble()
                } catch (e: Exception) {
                    0.0
                }
            }
        }
    }

    override fun observeSpecialists(): Flow<Outcome<List<Specialist>>> = flow {
        emit(getSpecialists())
    }

    override fun observeSpecialist(id: String): Flow<Outcome<Specialist>> = flow {
        emit(getSpecialist(id))
    }

    override suspend fun getSpecialists(): Outcome<List<Specialist>> {
        return Outcome.Success(transaction {
            SpecialistsTable.selectAll()
                .where { SpecialistsTable.isDeleted eq false }
                .orderBy(SpecialistsTable.createdAt to SortOrder.ASC)
                .map { it.toSpecialist() }
        })
    }

    override suspend fun getSpecialist(id: String): Outcome<Specialist> {
        return transaction {
            SpecialistsTable.selectAll().where { (SpecialistsTable.id eq id) and (SpecialistsTable.isDeleted eq false) }
                .map { Outcome.Success(it.toSpecialist()) }
                .singleOrNull() ?: Outcome.Error(Failure.ServerError(404, "Specialist not found"))
        }
    }

    override suspend fun refresh() {
        // Implementation for refreshing data if cached
    }

    override suspend fun submitReview(specialistId: String, rating: Int, comment: String): Outcome<Unit> = transaction {
        try {
            val reviewId = UUID.randomUUID().toString()
            val userId = UsersTable.selectAll().limit(1).map { it[UsersTable.id] }.singleOrNull() ?: "guest"
            
            ReviewsTable.insert {
                it[ReviewsTable.id] = reviewId
                it[ReviewsTable.userId] = userId
                it[ReviewsTable.targetId] = specialistId
                it[ReviewsTable.targetType] = "SPECIALIST"
                it[ReviewsTable.rating] = rating
                it[ReviewsTable.comment] = comment
                it[ReviewsTable.createdAt] = System.currentTimeMillis()
            }
            
            val user = UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()
            
            val event = DomainEvent.ReviewSubmitted(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                actorId = userId,
                message = "Review submitted for specialist $specialistId",
                reviewId = reviewId,
                targetId = specialistId,
                targetType = "SPECIALIST",
                rating = rating,
                userName = user?.get(UsersTable.name),
                userAvatarUrl = user?.get(UsersTable.avatarUrl)
            )
            outboxRepository.save(userId, json.encodeToString(event))
            
            Outcome.Success(Unit)
        } catch (e: Exception) {
            val message = e.message ?: "Failed to submit review"
            val friendlyMessage = if (message.contains("idx_reviews_user_target")) {
                "You have already submitted a review for this specialist."
            } else {
                message
            }
            Outcome.Error(Failure.ServerError(500, friendlyMessage))
        }
    }

    fun findById(id: String): SpecialistFetchResult = transaction {
        SpecialistsTable.selectAll().where { (SpecialistsTable.id eq id) and (SpecialistsTable.isDeleted eq false) }
            .map { it.toSpecialistDto() }
            .singleOrNull()?.let { SpecialistFetchResult.Success(it) } ?: SpecialistFetchResult.NotFound
    }

    fun findAll(salonId: String?, page: Int, pageSize: Int, query: String? = null): List<SpecialistDto> = transaction {
        val baseQuery = SpecialistsTable.selectAll().where { SpecialistsTable.isDeleted eq false }
        if (salonId != null) {
            baseQuery.andWhere { SpecialistsTable.salonId eq salonId }
        }

        query?.let { q ->
            val searchTerm = "%${q.lowercase()}%"
            baseQuery.andWhere {
                (SpecialistsTable.name.lowerCase() like searchTerm) or
                (SpecialistsTable.role.lowerCase() like searchTerm) or
                (SpecialistsTable.bio.lowerCase() like searchTerm)
            }
        }

        val items = baseQuery
            .orderBy(SpecialistsTable.createdAt to SortOrder.ASC)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map { it.toSpecialistDto() }

        items
    }

    fun findOnlineBySalon(salonId: String?, page: Int, pageSize: Int): List<SpecialistDto> = transaction {
        val query = SpecialistsTable.selectAll().where { 
            (SpecialistsTable.status eq "ONLINE") and (SpecialistsTable.isDeleted eq false)
        }
        if (salonId != null) {
            query.andWhere { SpecialistsTable.salonId eq salonId }
        }
        
        val items = query.limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map { it.toSpecialistDto() }
        
        items
    }

    fun create(request: CreateSpecialistRequestDto): SpecialistCreateResult = transaction {
        val id = UUID.randomUUID().toString()
        SpecialistsTable.insert {
            it[SpecialistsTable.id] = id
            it[SpecialistsTable.salonId] = request.salonId
            it[SpecialistsTable.name] = request.name
            it[SpecialistsTable.role] = request.role
            it[SpecialistsTable.imageUrl] = request.imageUrl
            it[SpecialistsTable.bio] = request.bio
            it[SpecialistsTable.customersServed] = request.customersServed
            it[SpecialistsTable.yearsOfExperience] = request.yearsOfExperience
            it[SpecialistsTable.userId] = request.userId
        }

        val serviceIds = request.serviceIds
        serviceIds.forEach { serviceId ->
            SpecialistServicesTable.insert {
                it[SpecialistServicesTable.id] = UUID.randomUUID().toString()
                it[SpecialistServicesTable.specialistId] = id
                it[SpecialistServicesTable.serviceId] = serviceId
            }
        }

        findById(id).let { 
            if (it is SpecialistFetchResult.Success) {
                // Sync new specialist to Stream
                repositoryScope.launch {
                    try {
                        streamSyncService.syncUser(
                            userId = it.specialist.id,
                            name = it.specialist.name,
                            role = UserRole.SPECIALIST,
                            avatarUrl = it.specialist.imageUrl
                        )
                    } catch (e: Exception) {
                        LoggerFactory.getLogger("SpecialistRepository").error("Failed to sync new specialist to Stream", e)
                    }
                }
                SpecialistCreateResult.Success(it.specialist)
            }
            else SpecialistCreateResult.Failure("Failed to retrieve created specialist")
        }
    }

    fun update(id: String, request: UpdateSpecialistRequestDto): SpecialistUpdateResult = transaction {
        val updatedRows = SpecialistsTable.update({ SpecialistsTable.id eq id }) {
            request.name?.let { name -> it[SpecialistsTable.name] = name }
            request.role?.let { role -> it[SpecialistsTable.role] = role }
            request.imageUrl?.let { imageUrl -> it[SpecialistsTable.imageUrl] = imageUrl }
            request.bio?.let { bio -> it[SpecialistsTable.bio] = bio }
            request.status?.let { status -> it[SpecialistsTable.status] = status }
            request.customersServed?.let { customersServed -> it[SpecialistsTable.customersServed] = customersServed }
            request.yearsOfExperience?.let { yearsOfExperience -> it[SpecialistsTable.yearsOfExperience] = yearsOfExperience }
        }

        val serviceIds = request.serviceIds
        if (serviceIds != null) {
            SpecialistServicesTable.deleteWhere { SpecialistServicesTable.specialistId eq id }
            serviceIds.forEach { serviceId ->
                SpecialistServicesTable.insert {
                    it[SpecialistServicesTable.id] = UUID.randomUUID().toString()
                    it[SpecialistServicesTable.specialistId] = id
                    it[SpecialistServicesTable.serviceId] = serviceId
                }
            }
        }

        if (updatedRows > 0) {
            val specialist = SpecialistsTable.selectAll().where { SpecialistsTable.id eq id }.singleOrNull()
            if (specialist != null) {
                val statusStr = specialist[SpecialistsTable.status]
                val event = DomainEvent.SpecialistStatusChanged(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    actorId = id,
                    message = "Specialist $id updated",
                    specialistId = id,
                    isAvailable = specialist[SpecialistsTable.isActive] && statusStr == "ONLINE",
                    status = statusStr
                )
                outboxRepository.save(null, json.encodeToString(event))
            }
            SpecialistUpdateResult.Success
        } else SpecialistUpdateResult.NotFound
    }

    fun updateStatus(id: String, status: String): SpecialistStatusUpdateResult = transaction {
        val updatedRows = SpecialistsTable.update({ SpecialistsTable.id eq id }) {
            it[SpecialistsTable.status] = status
        }
        if (updatedRows > 0) {
            val specialist = SpecialistsTable.selectAll().where { SpecialistsTable.id eq id }.singleOrNull()
            if (specialist != null) {
                val event = DomainEvent.SpecialistStatusChanged(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    actorId = id,
                    message = "Specialist $id status updated to $status",
                    specialistId = id,
                    isAvailable = specialist[SpecialistsTable.isActive] && status == "ONLINE",
                    status = status
                )
                outboxRepository.save(null, json.encodeToString(event))
            }
            SpecialistStatusUpdateResult.Success
        } else SpecialistStatusUpdateResult.NotFound
    }

    fun delete(id: String): SpecialistStatusUpdateResult = transaction {
        val updatedRows = SpecialistsTable.update({
            (SpecialistsTable.id eq id) and (SpecialistsTable.isDeleted eq false)
        }) {
            it[SpecialistsTable.isDeleted] = true
            it[SpecialistsTable.isActive] = false
            it[SpecialistsTable.status] = "AWAY"
        }
        if (updatedRows > 0) {
            val event = DomainEvent.SpecialistStatusChanged(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                actorId = id,
                message = "Specialist $id deleted",
                specialistId = id,
                isAvailable = false,
                status = "AWAY"
            )
            outboxRepository.save(id, json.encodeToString(event))
            SpecialistStatusUpdateResult.Success
        } else SpecialistStatusUpdateResult.NotFound
    }

    fun updateActiveStatus(id: String, isActive: Boolean): SpecialistStatusUpdateResult = transaction {
        val updatedRows = SpecialistsTable.update({ SpecialistsTable.id eq id }) {
            it[SpecialistsTable.isActive] = isActive
        }
        if (updatedRows > 0) {
            val specialist = SpecialistsTable.selectAll().where { SpecialistsTable.id eq id }.singleOrNull()
            if (specialist != null) {
                val statusStr = specialist[SpecialistsTable.status]
                val event = DomainEvent.SpecialistStatusChanged(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    actorId = id,
                    message = "Specialist $id active status updated to $isActive",
                    specialistId = id,
                    isAvailable = isActive && statusStr == "ONLINE",
                    status = statusStr
                )
                outboxRepository.save(null, json.encodeToString(event))
            }
            meterRegistry.counter("specialists_status_changes_total", "active", isActive.toString()).increment()
            SpecialistStatusUpdateResult.Success
        } else SpecialistStatusUpdateResult.NotFound
    }

    fun getDailyStats(id: String): SpecialistPerformanceDto = transaction {
        val now = System.currentTimeMillis()
        val period = 30L * 24 * 60 * 60 * 1000 // 30 days
        val currentStart = now - period
        val prevStart = currentStart - period

        val currentApps = AppointmentsTable.selectAll().where {
            (AppointmentsTable.specialistId eq id) and
            (AppointmentsTable.dateTime greaterEq currentStart) and
            (AppointmentsTable.dateTime less now)
        }.toList()

        val prevApps = AppointmentsTable.selectAll().where {
            (AppointmentsTable.specialistId eq id) and
            (AppointmentsTable.dateTime greaterEq prevStart) and
            (AppointmentsTable.dateTime less currentStart)
        }.toList()

        val completedStatus = listOf("COMPLETED", "CONFIRMED")

        val currentCompleted = currentApps.filter { it[AppointmentsTable.status] in completedStatus }
        val currentRevenue = currentCompleted.sumOf { it[AppointmentsTable.totalAmount] }
        val currentCount = currentCompleted.count()

        val prevCompleted = prevApps.filter { it[AppointmentsTable.status] in completedStatus }
        val prevRevenue = prevCompleted.sumOf { it[AppointmentsTable.totalAmount] }

        val revenueGrowth = calculateGrowth(currentRevenue, prevRevenue)

        val name = SpecialistsTable.select(SpecialistsTable.name)
            .where { SpecialistsTable.id eq id }
            .map { it[SpecialistsTable.name] }
            .singleOrNull() ?: ""

        val totalCurrent = currentApps.count()
        val completionRate = if (totalCurrent > 0) {
            (currentCount.toDouble() / totalCurrent) * 100.0
        } else 0.0

        SpecialistPerformanceDto(
            specialistId = id,
            name = name,
            appointmentCount = currentCount,
            completionRate = completionRate,
            revenue = currentRevenue,
            revenueGrowth = revenueGrowth,
            targetAchievement = (currentCount.toDouble() / 20.0).coerceIn(0.0, 1.0)
        )
    }

    private fun calculateGrowth(current: Double, previous: Double): Double {
        return if (previous == 0.0) {
            if (current > 0) 100.0 else 0.0
        } else {
            ((current - previous) / previous) * 100.0
        }
    }

    fun updateShifts(id: String, shifts: List<SpecialistShiftDto>): SpecialistStatusUpdateResult = transaction {
        SpecialistShiftsTable.deleteWhere { specialistId eq id }
        shifts.forEach { shift ->
            SpecialistShiftsTable.insert {
                it[SpecialistShiftsTable.id] = UUID.randomUUID().toString()
                it[SpecialistShiftsTable.specialistId] = id
                it[SpecialistShiftsTable.dayOfWeek] = shift.dayOfWeek
                it[SpecialistShiftsTable.startTime] = shift.startTime
                it[SpecialistShiftsTable.endTime] = shift.endTime
                it[SpecialistShiftsTable.isActive] = shift.isWorkingDay
            }
        }

        val specialist = SpecialistsTable.selectAll().where { SpecialistsTable.id eq id }.singleOrNull()
        if (specialist != null) {
            val event = DomainEvent.SpecialistStatusChanged(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                actorId = id,
                message = "Specialist $id shifts updated",
                specialistId = id,
                isAvailable = specialist[SpecialistsTable.isActive],
                status = specialist[SpecialistsTable.status]
            )
            outboxRepository.save(id, json.encodeToString(event))
        }

        SpecialistStatusUpdateResult.Success
    }

    fun getAvatarPath(id: String): String? = transaction {
        SpecialistsTable.select(SpecialistsTable.imageUrl)
            .where { SpecialistsTable.id eq id }
            .map { it[SpecialistsTable.imageUrl] }
            .singleOrNull()
    }

    fun getGalleryPath(id: String, index: Int): String? = transaction {
        SpecialistsTable.select(SpecialistsTable.gallery)
            .where { SpecialistsTable.id eq id }
            .map { it[SpecialistsTable.gallery] }
            .singleOrNull()?.getOrNull(index)
    }

    private fun ResultRow.toSpecialistDto() = transaction {
        val id = this@toSpecialistDto[SpecialistsTable.id]
        
        val services = (SpecialistServicesTable innerJoin ServicesTable)
            .selectAll().where { SpecialistServicesTable.specialistId eq id }
            .map { 
                ServiceDto(
                    id = it[ServicesTable.id],
                    name = it[ServicesTable.name],
                    description = it[ServicesTable.description],
                    price = it[ServicesTable.price],
                    durationMinutes = it[ServicesTable.durationMinutes],
                    imageUrl = it[ServicesTable.imageUrl],
                    categoryId = it[ServicesTable.categoryId],
                    subCategory = it[ServicesTable.subCategory],
                    rating = it[ServicesTable.rating],
                    isActive = it[ServicesTable.isActive]
                )
            }

        val directReviews = (ReviewsTable innerJoin UsersTable)
            .selectAll().where { 
                (ReviewsTable.targetId eq id) and 
                (ReviewsTable.targetType eq "SPECIALIST") and
                (ReviewsTable.isVisible eq true)
            }
            .map { it.toSpecialistReviewDto() }

        val appointmentReviews = (ReviewsTable innerJoin UsersTable)
            .join(AppointmentsTable, JoinType.INNER, ReviewsTable.targetId, AppointmentsTable.id)
            .selectAll().where {
                (ReviewsTable.targetType eq "APPOINTMENT") and
                (AppointmentsTable.specialistId eq id) and
                (ReviewsTable.isVisible eq true)
            }
            .map { it.toSpecialistReviewDto() }

        val reviews = (directReviews + appointmentReviews).sortedByDescending { it.createdAtEpochSeconds }

        val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0

        SpecialistDto(
            id = id,
            userId = this@toSpecialistDto[SpecialistsTable.userId],
            name = this@toSpecialistDto[SpecialistsTable.name],
            role = this@toSpecialistDto[SpecialistsTable.role],
            imageUrl = this@toSpecialistDto[SpecialistsTable.imageUrl],
            bio = this@toSpecialistDto[SpecialistsTable.bio],
            rating = avgRating,
            salonId = this@toSpecialistDto[SpecialistsTable.salonId],
            status = this@toSpecialistDto[SpecialistsTable.status],
            isActive = this@toSpecialistDto[SpecialistsTable.isActive],
            customersServed = countCustomersServed(id),
            yearsOfExperience = this@toSpecialistDto[SpecialistsTable.yearsOfExperience],
            isVerified = this@toSpecialistDto[SpecialistsTable.isVerified],
            services = services,
            reviews = reviews,
            isOnline = this@toSpecialistDto[SpecialistsTable.status] == "ONLINE"
        )
    }

    private fun ResultRow.toSpecialistReviewDto() = SpecialistReviewDto(
        id = this[ReviewsTable.id],
        userName = this[UsersTable.name],
        userAvatarUrl = this[UsersTable.avatarUrl],
        rating = this[ReviewsTable.rating],
        comment = this[ReviewsTable.comment] ?: "",
        createdAtEpochSeconds = this[ReviewsTable.createdAt] / 1000
    )

    /**
     * Real number of customers this specialist has served, derived from
     * completed/confirmed appointments rather than the static seeded column.
     */
    private fun countCustomersServed(specialistId: String): Int =
        AppointmentsTable.selectAll().where {
            (AppointmentsTable.specialistId eq specialistId) and
                (AppointmentsTable.status inList listOf(
                    AppointmentStatus.COMPLETED.name,
                    AppointmentStatus.CONFIRMED.name
                ))
        }.count().toInt()

    private fun ResultRow.toSpecialist() = transaction {
        val id = this@toSpecialist[SpecialistsTable.id]

        val services = (SpecialistServicesTable innerJoin ServicesTable)
            .selectAll().where { SpecialistServicesTable.specialistId eq id }
            .map { 
                Service(
                    id = it[ServicesTable.id],
                    name = it[ServicesTable.name],
                    description = it[ServicesTable.description],
                    durationMinutes = it[ServicesTable.durationMinutes],
                    price = it[ServicesTable.price],
                    categoryId = it[ServicesTable.categoryId],
                    imageUrl = it[ServicesTable.imageUrl]
                )
            }

        val directReviews = (ReviewsTable innerJoin UsersTable)
            .selectAll().where { 
                (ReviewsTable.targetId eq id) and 
                (ReviewsTable.targetType eq "SPECIALIST") and
                (ReviewsTable.isVisible eq true)
            }
            .map { it.toSpecialistReview() }

        val appointmentReviews = (ReviewsTable innerJoin UsersTable)
            .join(AppointmentsTable, JoinType.INNER, ReviewsTable.targetId, AppointmentsTable.id)
            .selectAll().where {
                (ReviewsTable.targetType eq "APPOINTMENT") and
                (AppointmentsTable.specialistId eq id) and
                (ReviewsTable.isVisible eq true)
            }
            .map { it.toSpecialistReview() }

        val reviews = (directReviews + appointmentReviews).sortedByDescending { it.createdAtEpochSeconds }

        val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average() else 0.0

        Specialist(
            id = id,
            name = this@toSpecialist[SpecialistsTable.name],
            role = this@toSpecialist[SpecialistsTable.role],
            salonId = this@toSpecialist[SpecialistsTable.salonId],
            rating = avgRating,
            imageUrl = this@toSpecialist[SpecialistsTable.imageUrl],
            isOnline = this@toSpecialist[SpecialistsTable.status] == "ONLINE",
            isVerified = this@toSpecialist[SpecialistsTable.isVerified],
            bio = this@toSpecialist[SpecialistsTable.bio] ?: "",
            customersCount = countCustomersServed(id),
            yearsOfExperience = this@toSpecialist[SpecialistsTable.yearsOfExperience],
            services = services,
            reviews = reviews
        )
    }

    private fun ResultRow.toSpecialistReview() = SpecialistReview(
        id = this[ReviewsTable.id],
        userName = this[UsersTable.name],
        userAvatarUrl = this[UsersTable.avatarUrl],
        rating = this[ReviewsTable.rating],
        comment = this[ReviewsTable.comment],
        createdAtEpochSeconds = this[ReviewsTable.createdAt] / 1000
    )
}
