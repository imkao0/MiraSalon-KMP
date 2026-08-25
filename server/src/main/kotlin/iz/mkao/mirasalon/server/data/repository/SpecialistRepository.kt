package iz.mkao.mirasalon.server.data.repository

import io.micrometer.core.instrument.MeterRegistry
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.model.SpecialistReview
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.CreateSpecialistRequestDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistPerformanceDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistReviewDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistShiftDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateSpecialistRequestDto
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import iz.mkao.mirasalon.server.data.tables.AppointmentsTable
import iz.mkao.mirasalon.server.data.tables.ReviewsTable
import iz.mkao.mirasalon.server.data.tables.ServicesTable
import iz.mkao.mirasalon.server.data.tables.SpecialistServicesTable
import iz.mkao.mirasalon.server.data.tables.SpecialistsTable
import iz.mkao.mirasalon.server.data.tables.UsersTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
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
    private val availabilityRepository: SpecialistAvailabilityRepository,
    private val meterRegistry: MeterRegistry
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
            val rows = SpecialistsTable.selectAll()
                .where { SpecialistsTable.isDeleted eq false }
                .orderBy(SpecialistsTable.createdAt to SortOrder.ASC)
                .toList()
            
            val ids = rows.map { it[SpecialistsTable.id] }
            val servicesMap = fetchServicesBatch(ids)
            val reviewsMap = fetchReviewsBatch(ids)
            val countsMap = fetchCustomerCountsBatch(ids)
            
            rows.map { row ->
                val id = row[SpecialistsTable.id]
                mapToSpecialist(
                    row = row,
                    services = servicesMap[id] ?: emptyList(),
                    reviews = reviewsMap[id] ?: emptyList(),
                    customerCount = countsMap[id] ?: 0
                )
            }
        })
    }

    override suspend fun getSpecialist(id: String): Outcome<Specialist> {
        return transaction {
            val row = SpecialistsTable.selectAll().where { (SpecialistsTable.id eq id) and (SpecialistsTable.isDeleted eq false) }
                .singleOrNull() ?: return@transaction Outcome.Error(Failure.ServerError(404, "Specialist not found"))
            
            val servicesMap = fetchServicesBatch(listOf(id))
            val reviewsMap = fetchReviewsBatch(listOf(id))
            val countsMap = fetchCustomerCountsBatch(listOf(id))
            
            Outcome.Success(mapToSpecialist(
                row = row,
                services = servicesMap[id] ?: emptyList(),
                reviews = reviewsMap[id] ?: emptyList(),
                customerCount = countsMap[id] ?: 0
            ))
        }
    }

    override suspend fun refresh() {
        // Implementation for refreshing data for cached
    }

    override suspend fun submitReview(specialistId: String, rating: Int, comment: String, userId: String?): Outcome<Unit> = transaction {
        try {
            val finalUserId = userId ?: return@transaction Outcome.Error(Failure.ClientError(401, "Authentication required"))

            // Business Rule: Check if user has had a completed appointment with this specialist
            val appointmentCount = AppointmentsTable.selectAll().where {
                (AppointmentsTable.userId eq finalUserId) and
                (AppointmentsTable.specialistId eq specialistId) and
                (AppointmentsTable.status eq "COMPLETED")
            }.count()

            if (appointmentCount == 0L) {
                return@transaction Outcome.Error(Failure.ServerError(403, "You can only review specialists you have had a completed appointment with."))
            }

            // Business Rule: Prevent more reviews than completed appointments
            val reviewCount = ReviewsTable.selectAll().where {
                (ReviewsTable.userId eq finalUserId) and
                (ReviewsTable.targetId eq specialistId) and
                (ReviewsTable.targetType eq "SPECIALIST")
            }.count()

            if (reviewCount >= appointmentCount) {
                return@transaction Outcome.Error(Failure.ServerError(409, "You have already submitted reviews for all your recent appointments with this specialist."))
            }

            val reviewId = UUID.randomUUID().toString()
            
            ReviewsTable.insert {
                it[ReviewsTable.id] = reviewId
                it[ReviewsTable.userId] = finalUserId
                it[ReviewsTable.targetId] = specialistId
                it[ReviewsTable.targetType] = "SPECIALIST"
                it[ReviewsTable.rating] = rating
                it[ReviewsTable.comment] = comment
                it[ReviewsTable.createdAt] = System.currentTimeMillis()
            }

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
        val row = SpecialistsTable.selectAll().where { (SpecialistsTable.id eq id) and (SpecialistsTable.isDeleted eq false) }
            .singleOrNull() ?: return@transaction SpecialistFetchResult.NotFound

        val servicesMap = fetchServicesDtoBatch(listOf(id))
        val reviewsMap = fetchReviewsDtoBatch(listOf(id))
        val countsMap = fetchCustomerCountsBatch(listOf(id))

        SpecialistFetchResult.Success(mapToSpecialistDto(
            row = row,
            services = servicesMap[id] ?: emptyList(),
            reviews = reviewsMap[id] ?: emptyList(),
            customerCount = countsMap[id] ?: 0
        ))
    }

    /**
     * Finds a specialist by their *linked user account* id ([SpecialistsTable.userId]).
     * Chat partitions are sometimes keyed by the specialist's user UUID (or a "user-…"
     * id) rather than the "spec-…" row id, so this lets the chat layer resolve the real
     * specialist record regardless of which identifier the client used.
     */
    fun findByUserId(userId: String): SpecialistFetchResult = transaction {
        val row = SpecialistsTable.selectAll()
            .where { (SpecialistsTable.userId eq userId) and (SpecialistsTable.isDeleted eq false) }
            .singleOrNull() ?: return@transaction SpecialistFetchResult.NotFound

        val id = row[SpecialistsTable.id]
        val servicesMap = fetchServicesDtoBatch(listOf(id))
        val reviewsMap = fetchReviewsDtoBatch(listOf(id))
        val countsMap = fetchCustomerCountsBatch(listOf(id))

        SpecialistFetchResult.Success(mapToSpecialistDto(
            row = row,
            services = servicesMap[id] ?: emptyList(),
            reviews = reviewsMap[id] ?: emptyList(),
            customerCount = countsMap[id] ?: 0
        ))
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

        val rows = baseQuery
            .orderBy(SpecialistsTable.createdAt to SortOrder.ASC)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .toList()
            
        val ids = rows.map { it[SpecialistsTable.id] }
        val servicesMap = fetchServicesDtoBatch(ids)
        val reviewsMap = fetchReviewsDtoBatch(ids)
        val countsMap = fetchCustomerCountsBatch(ids)

        rows.map { row ->
            val id = row[SpecialistsTable.id]
            mapToSpecialistDto(
                row = row,
                services = servicesMap[id] ?: emptyList(),
                reviews = reviewsMap[id] ?: emptyList(),
                customerCount = countsMap[id] ?: 0
            )
        }
    }

    fun findOnlineBySalon(salonId: String?, page: Int, pageSize: Int): List<SpecialistDto> = transaction {
        val query = SpecialistsTable.selectAll().where { 
            (SpecialistsTable.status eq "ONLINE") and (SpecialistsTable.isDeleted eq false)
        }
        if (salonId != null) {
            query.andWhere { SpecialistsTable.salonId eq salonId }
        }
        
        val rows = query.limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .toList()
            
        val ids = rows.map { it[SpecialistsTable.id] }
        val servicesMap = fetchServicesDtoBatch(ids)
        val reviewsMap = fetchReviewsDtoBatch(ids)
        val countsMap = fetchCustomerCountsBatch(ids)

        rows.map { row ->
            val id = row[SpecialistsTable.id]
            mapToSpecialistDto(
                row = row,
                services = servicesMap[id] ?: emptyList(),
                reviews = reviewsMap[id] ?: emptyList(),
                customerCount = countsMap[id] ?: 0
            )
        }
    }

    fun create(request: CreateSpecialistRequestDto): SpecialistCreateResult = transaction {
        val id = UUID.randomUUID().toString()

        // Ensure the linked user exists to satisfy foreign key constraint
        request.userId?.let { userId ->
            val userExists = UsersTable.selectAll().where { UsersTable.id eq userId }.any()
            if (!userExists) {
                UsersTable.insert {
                    it[UsersTable.id] = userId
                    it[UsersTable.name] = request.name
                    it[UsersTable.email] = "specialist.${userId}@mirasalon.com"
                    it[UsersTable.passwordHash] = "PLACEHOLDER" // Specialist should reset password via "Forgot Password" or admin
                    it[UsersTable.role] = "SPECIALIST"
                    it[UsersTable.createdAt] = System.currentTimeMillis()
                }
            }
        }

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
                // Dispatch Realtime Event
                val event = DomainEvent.SpecialistCreated(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    actorId = null, 
                    message = "New specialist created: ${it.specialist.name}",
                    specialistId = it.specialist.id
                )
                outboxRepository.save(null, DomainEventCodec.encode(event))
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
                    message = "Specialist ${specialist[SpecialistsTable.name]} profile updated",
                    specialistId = id,
                    isAvailable = specialist[SpecialistsTable.isActive] && statusStr == "ONLINE",
                    status = statusStr
                )
                outboxRepository.save(null, DomainEventCodec.encode(event))
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
                    message = "Specialist ${specialist[SpecialistsTable.name]} is now $status",
                    specialistId = id,
                    isAvailable = specialist[SpecialistsTable.isActive] && status == "ONLINE",
                    status = status
                )
                outboxRepository.save(null, DomainEventCodec.encode(event))
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
                message = "Specialist $id has been removed",
                specialistId = id,
                isAvailable = false,
                status = "AWAY"
            )
            // Specialists are not in users table, pass null for userId
            outboxRepository.save(null, DomainEventCodec.encode(event))
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
                    message = "Specialist ${specialist[SpecialistsTable.name]} status changed to ${if (isActive) "Active" else "Inactive"}",
                    specialistId = id,
                    isAvailable = isActive && statusStr == "ONLINE",
                    status = statusStr
                )
                outboxRepository.save(null, DomainEventCodec.encode(event))
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
        availabilityRepository.updateShifts(id, shifts)

        val specialist = SpecialistsTable.selectAll().where { SpecialistsTable.id eq id }.singleOrNull()
        if (specialist != null) {
            val event = DomainEvent.SpecialistStatusChanged(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                actorId = id,
                message = "Specialist ${specialist[SpecialistsTable.name]} availability updated",
                specialistId = id,
                isAvailable = specialist[SpecialistsTable.isActive],
                status = specialist[SpecialistsTable.status]
            )
            // Specialists are not in users table, pass null for userId
            outboxRepository.save(null, DomainEventCodec.encode(event))
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

    private fun fetchServicesDtoBatch(ids: List<String>): Map<String, List<ServiceDto>> {
        if (ids.isEmpty()) return emptyMap()
        return (SpecialistServicesTable innerJoin ServicesTable)
            .selectAll().where { SpecialistServicesTable.specialistId inList ids }
            .map { it[SpecialistServicesTable.specialistId] to it.toServiceDto() }
            .groupBy({ it.first }, { it.second })
    }

    private fun fetchServicesBatch(ids: List<String>): Map<String, List<Service>> {
        if (ids.isEmpty()) return emptyMap()
        return (SpecialistServicesTable innerJoin ServicesTable)
            .selectAll().where { SpecialistServicesTable.specialistId inList ids }
            .map { it[SpecialistServicesTable.specialistId] to it.toService() }
            .groupBy({ it.first }, { it.second })
    }

    private fun fetchReviewsDtoBatch(ids: List<String>): Map<String, List<SpecialistReviewDto>> {
        if (ids.isEmpty()) return emptyMap()
        val directReviews = (ReviewsTable innerJoin UsersTable)
            .selectAll().where { 
                (ReviewsTable.targetId inList ids) and 
                (ReviewsTable.targetType eq "SPECIALIST") and
                (ReviewsTable.isVisible eq true)
            }
            .map { it[ReviewsTable.targetId] to it.toSpecialistReviewDto() }

        val appointmentReviews = (ReviewsTable innerJoin UsersTable)
            .join(AppointmentsTable, JoinType.INNER, ReviewsTable.targetId, AppointmentsTable.id)
            .selectAll().where {
                (ReviewsTable.targetType eq "APPOINTMENT") and
                (AppointmentsTable.specialistId inList ids) and
                (ReviewsTable.isVisible eq true)
            }
            .map { it[AppointmentsTable.specialistId] to it.toSpecialistReviewDto() }

        return (directReviews + appointmentReviews)
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.sortedByDescending { it.createdAtEpochSeconds } }
    }

    private fun fetchReviewsBatch(ids: List<String>): Map<String, List<SpecialistReview>> {
        if (ids.isEmpty()) return emptyMap()
        val directReviews = (ReviewsTable innerJoin UsersTable)
            .selectAll().where { 
                (ReviewsTable.targetId inList ids) and 
                (ReviewsTable.targetType eq "SPECIALIST") and
                (ReviewsTable.isVisible eq true)
            }
            .map { it[ReviewsTable.targetId] to it.toSpecialistReview() }

        val appointmentReviews = (ReviewsTable innerJoin UsersTable)
            .join(AppointmentsTable, JoinType.INNER, ReviewsTable.targetId, AppointmentsTable.id)
            .selectAll().where {
                (ReviewsTable.targetType eq "APPOINTMENT") and
                (AppointmentsTable.specialistId inList ids) and
                (ReviewsTable.isVisible eq true)
            }
            .map { it[AppointmentsTable.specialistId] to it.toSpecialistReview() }

        return (directReviews + appointmentReviews)
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.sortedByDescending { it.createdAtEpochSeconds } }
    }

    private fun fetchCustomerCountsBatch(ids: List<String>): Map<String, Int> {
        if (ids.isEmpty()) return emptyMap()
        val countColumn = AppointmentsTable.id.count()
        return AppointmentsTable
            .select(AppointmentsTable.specialistId, countColumn)
            .where {
                (AppointmentsTable.specialistId inList ids) and
                (AppointmentsTable.status inList listOf("COMPLETED", "CONFIRMED"))
            }
            .groupBy(AppointmentsTable.specialistId)
            .associate { it[AppointmentsTable.specialistId] to it[countColumn].toInt() }
    }

    private fun mapToSpecialistDto(
        row: ResultRow,
        services: List<ServiceDto>,
        reviews: List<SpecialistReviewDto>,
        customerCount: Int
    ): SpecialistDto {
        val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0
        return SpecialistDto(
            id = row[SpecialistsTable.id],
            userId = row[SpecialistsTable.userId],
            name = row[SpecialistsTable.name],
            role = row[SpecialistsTable.role],
            imageUrl = row[SpecialistsTable.imageUrl],
            bio = row[SpecialistsTable.bio],
            rating = avgRating,
            salonId = row[SpecialistsTable.salonId],
            status = row[SpecialistsTable.status],
            isActive = row[SpecialistsTable.isActive],
            customersServed = customerCount,
            yearsOfExperience = row[SpecialistsTable.yearsOfExperience],
            isVerified = row[SpecialistsTable.isVerified],
            services = services,
            reviews = reviews,
            isOnline = row[SpecialistsTable.status] == "ONLINE"
        )
    }

    private fun mapToSpecialist(
        row: ResultRow,
        services: List<Service>,
        reviews: List<SpecialistReview>,
        customerCount: Int
    ): Specialist {
        val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average() else 0.0
        return Specialist(
            id = row[SpecialistsTable.id],
            name = row[SpecialistsTable.name],
            role = row[SpecialistsTable.role],
            salonId = row[SpecialistsTable.salonId],
            rating = avgRating,
            imageUrl = row[SpecialistsTable.imageUrl],
            isOnline = row[SpecialistsTable.status] == "ONLINE",
            isVerified = row[SpecialistsTable.isVerified],
            bio = row[SpecialistsTable.bio] ?: "",
            customersCount = customerCount,
            yearsOfExperience = row[SpecialistsTable.yearsOfExperience],
            services = services,
            reviews = reviews,
            userId = row[SpecialistsTable.userId]
        )
    }

    private fun ResultRow.toServiceDto() = ServiceDto(
        id = this[ServicesTable.id],
        name = this[ServicesTable.name],
        description = this[ServicesTable.description],
        price = this[ServicesTable.price],
        durationMinutes = this[ServicesTable.durationMinutes],
        imageUrl = "/v1/api/services/${this[ServicesTable.id]}/image",
        categoryId = this[ServicesTable.categoryId],
        subCategory = this[ServicesTable.subCategory],
        rating = this[ServicesTable.rating],
        isActive = this[ServicesTable.isActive]
    )

    private fun ResultRow.toService() = Service(
        id = this[ServicesTable.id],
        name = this[ServicesTable.name],
        description = this[ServicesTable.description],
        durationMinutes = this[ServicesTable.durationMinutes],
        price = this[ServicesTable.price],
        categoryId = this[ServicesTable.categoryId],
        imageUrl = this[ServicesTable.imageUrl] ?: ApiEndpoints.getServicePlaceholder(this[ServicesTable.name])
    )

    /**
     * Real number of customers this specialist has served, derived from
     * completed/confirmed appointments rather than the static seeded column.
     */

    private fun ResultRow.toSpecialistReviewDto() = SpecialistReviewDto(
        id = this[ReviewsTable.id],
        userName = this[UsersTable.name],
        userAvatarUrl = this[UsersTable.avatarUrl],
        rating = this[ReviewsTable.rating],
        comment = this[ReviewsTable.comment] ?: "",
        createdAtEpochSeconds = this[ReviewsTable.createdAt] / 1000
    )

    private fun ResultRow.toSpecialistReview() = SpecialistReview(
        id = this[ReviewsTable.id],
        userName = this[UsersTable.name],
        userAvatarUrl = this[UsersTable.avatarUrl],
        rating = this[ReviewsTable.rating],
        comment = this[ReviewsTable.comment],
        createdAtEpochSeconds = this[ReviewsTable.createdAt] / 1000
    )
}
