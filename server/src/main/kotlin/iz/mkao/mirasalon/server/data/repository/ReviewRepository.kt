package iz.mkao.mirasalon.server.data.repository

import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.AdminReviewDto
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto
import iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest
import iz.mkao.mirasalon.server.data.tables.AppointmentsTable
import iz.mkao.mirasalon.server.data.tables.ReviewsTable
import iz.mkao.mirasalon.server.data.tables.UsersTable
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
import java.util.UUID

sealed class ReviewCreationResult {
    data class Success(val reviewId: String) : ReviewCreationResult()
    data class Error(val message: String) : ReviewCreationResult()
}

sealed class AdminReplyResult {
    data object Success : AdminReplyResult()
    data object NotFound : AdminReplyResult()
    data class Error(val message: String) : AdminReplyResult()
}

class ReviewRepository(
    private val outboxRepository: OutboxRepository,
    private val json: Json
) {

    fun findByTarget(targetId: String, targetType: String): List<ReviewDto> = transaction {
        (ReviewsTable innerJoin UsersTable)
            .selectAll()
            .where { 
                (ReviewsTable.targetId eq targetId) and 
                (ReviewsTable.targetType eq targetType) and
                (ReviewsTable.isVisible eq true)
            }
            .orderBy(ReviewsTable.createdAt, SortOrder.DESC)
            .map { it.toReviewDto() }
    }

    fun findByTargetPaginated(targetId: String, targetType: String, page: Int, pageSize: Int): PagedResponse<ReviewDto> = transaction {
        val query = (ReviewsTable innerJoin UsersTable)
            .selectAll()
            .where { 
                (ReviewsTable.targetId eq targetId) and 
                (ReviewsTable.targetType eq targetType) and
                (ReviewsTable.isVisible eq true)
            }
        
        val total = query.count()
        val items = query.orderBy(ReviewsTable.createdAt, SortOrder.DESC)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map { it.toReviewDto() }
        
        val totalPages = if (pageSize > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
        PagedResponse(items, total, page, pageSize, totalPages)
    }

    fun findBySpecialistPaginated(specialistId: String, page: Int, pageSize: Int): PagedResponse<ReviewDto> = transaction {
        val query = ReviewsTable
            .join(UsersTable, JoinType.INNER, ReviewsTable.userId, UsersTable.id)
            .join(AppointmentsTable, JoinType.INNER, ReviewsTable.targetId, AppointmentsTable.id)
            .selectAll()
            .where { 
                (ReviewsTable.targetType eq "APPOINTMENT") and 
                (AppointmentsTable.specialistId eq specialistId) and 
                (ReviewsTable.isVisible eq true)
            }
        
        val total = query.count()
        val items = query.orderBy(ReviewsTable.createdAt, SortOrder.DESC)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map { it.toReviewDto() }
        
        val totalPages = if (pageSize > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
        PagedResponse(items, total, page, pageSize, totalPages)
    }

    fun findByUser(userId: String): List<ReviewDto> = transaction {
        (ReviewsTable innerJoin UsersTable)
            .selectAll()
            .where { ReviewsTable.userId eq userId }
            .orderBy(ReviewsTable.createdAt, SortOrder.DESC)
            .map { it.toReviewDto() }
    }

    fun create(userId: String, request: SubmitReviewRequest): ReviewDto = transaction {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        ReviewsTable.insert {
            it[ReviewsTable.id] = id
            it[ReviewsTable.userId] = userId
            it[ReviewsTable.targetId] = request.targetId
            it[ReviewsTable.targetType] = request.targetType
            it[ReviewsTable.rating] = request.rating
            it[ReviewsTable.comment] = request.comment
            it[ReviewsTable.createdAt] = now
        }
        
        val user = UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()
        
        val event = DomainEvent.ReviewSubmitted(
            eventId = UUID.randomUUID().toString(),
            timestamp = now,
            actorId = userId,
            message = "Review submitted for ${request.targetType} ${request.targetId}",
            reviewId = id,
            targetId = request.targetId,
            targetType = request.targetType,
            rating = request.rating,
            userName = user?.get(UsersTable.name),
            userAvatarUrl = user?.get(UsersTable.avatarUrl)
        )
        outboxRepository.save(userId, json.encodeToString(event))
        
        if (request.targetType == "APPOINTMENT") {
            AppointmentsTable.update({ AppointmentsTable.id eq request.targetId }) {
                it[isReviewed] = true
            }
        }

        (ReviewsTable innerJoin UsersTable)
            .selectAll()
            .where { ReviewsTable.id eq id }
            .map { it.toReviewDto() }
            .single()
    }

    fun createReview(userId: String, targetId: String, targetType: String, rating: Int, comment: String?, imageUrl: String?): ReviewCreationResult = transaction {
        try {
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            ReviewsTable.insert {
                it[ReviewsTable.id] = id
                it[ReviewsTable.userId] = userId
                it[ReviewsTable.targetId] = targetId
                it[ReviewsTable.targetType] = targetType
                it[ReviewsTable.rating] = rating
                it[ReviewsTable.comment] = comment
                it[ReviewsTable.imageUrl] = imageUrl
                it[ReviewsTable.createdAt] = now
            }
            
            val user = UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()
            
            val event = DomainEvent.ReviewSubmitted(
                eventId = UUID.randomUUID().toString(),
                timestamp = now,
                actorId = userId,
                message = "Review submitted for $targetType $targetId",
                reviewId = id,
                targetId = targetId,
                targetType = targetType,
                rating = rating,
                userName = user?.get(UsersTable.name),
                userAvatarUrl = user?.get(UsersTable.avatarUrl)
            )
            outboxRepository.save(userId, json.encodeToString(event))

            if (targetType == "APPOINTMENT") {
                val updated = AppointmentsTable.update({ AppointmentsTable.id eq targetId }) {
                    it[isReviewed] = true
                }
                org.slf4j.LoggerFactory.getLogger("ReviewRepo").info("Updated appointment {} isReviewed to true, rows affected: {}", targetId, updated)
            }
            
            ReviewCreationResult.Success(id)
        } catch (e: Exception) {
            val message = e.message ?: "Unknown error"
            if (message.contains("idx_reviews_user_target")) {
                ReviewCreationResult.Error("You have already submitted a review for this item.")
            } else {
                ReviewCreationResult.Error(message)
            }
        }
    }

    fun findAllPaginated(page: Int, pageSize: Int, query: String? = null): PagedResponse<AdminReviewDto> = transaction {
        val baseQuery = (ReviewsTable innerJoin UsersTable).selectAll()

        if (query != null) {
            val searchTerm = "%${query.lowercase()}%"
            baseQuery.andWhere {
                (UsersTable.name.lowerCase() like searchTerm) or
                (ReviewsTable.comment.lowerCase() like searchTerm)
            }
        }

        val total = baseQuery.count()
        val items = baseQuery
            .orderBy(ReviewsTable.createdAt, SortOrder.DESC)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map { it.toAdminReviewDto() }

        val totalPages = if (pageSize > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
        PagedResponse(items, total, page, pageSize, totalPages)
    }

    fun addAdminReply(reviewId: String, reply: String): AdminReplyResult = transaction {
        val updated = ReviewsTable.update({ ReviewsTable.id eq reviewId }) {
            it[adminReply] = reply
            it[adminReplyAt] = System.currentTimeMillis()
        }
        if (updated > 0) AdminReplyResult.Success
        else AdminReplyResult.NotFound
    }

    fun updateVisibility(reviewId: String, isVisible: Boolean) = transaction {
        ReviewsTable.update({ ReviewsTable.id eq reviewId }) {
            it[this.isVisible] = isVisible
        }
    }

    fun deleteReview(reviewId: String) = transaction {
        ReviewsTable.deleteWhere { id eq reviewId }
    }

    private fun ResultRow.toReviewDto() = ReviewDto(
        id = this[ReviewsTable.id],
        userName = this[UsersTable.name],
        userAvatarUrl = this[UsersTable.avatarUrl],
        rating = this[ReviewsTable.rating],
        comment = this[ReviewsTable.comment],
        createdAtEpochSeconds = this[ReviewsTable.createdAt] / 1000,
        targetId = this[ReviewsTable.targetId],
        targetType = this[ReviewsTable.targetType],
        imageUrl = this[ReviewsTable.imageUrl]
    )

    private fun ResultRow.toAdminReviewDto() = AdminReviewDto(
        id = this[ReviewsTable.id],
        userId = this[ReviewsTable.userId],
        userName = this[UsersTable.name],
        targetId = this[ReviewsTable.targetId],
        targetType = this[ReviewsTable.targetType],
        targetName = "",
        rating = this[ReviewsTable.rating],
        comment = this[ReviewsTable.comment],
        adminReply = this[ReviewsTable.adminReply],
        createdAt = this[ReviewsTable.createdAt],
        isVisible = this[ReviewsTable.isVisible]
    )
}
