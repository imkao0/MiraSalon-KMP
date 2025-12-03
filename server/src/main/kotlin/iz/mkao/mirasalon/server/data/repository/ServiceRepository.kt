package iz.mkao.mirasalon.server.data.repository

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.domain.model.ServiceFilter
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.CreateServiceRequestDto
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceCategoryDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateServiceRequestDto
import iz.mkao.mirasalon.server.data.tables.AppointmentsTable
import iz.mkao.mirasalon.server.data.tables.ReviewsTable
import iz.mkao.mirasalon.server.data.tables.ServiceCategoriesTable
import iz.mkao.mirasalon.server.data.tables.ServicesTable
import iz.mkao.mirasalon.server.data.tables.UsersTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID
import iz.mkao.mirasalon.core.domain.repository.ServiceRepository as CoreServiceRepository

sealed class ServiceFetchResult {
    data class Success(val service: ServiceDto) : ServiceFetchResult()
    data object NotFound : ServiceFetchResult()
}

sealed class ServiceCreateResult {
    data class Success(val service: ServiceDto) : ServiceCreateResult()
    data class Failure(val message: String) : ServiceCreateResult()
}

sealed class ServiceUpdateResult {
    data object Success : ServiceUpdateResult()
    data object NotFound : ServiceUpdateResult()
    data class Failure(val message: String) : ServiceUpdateResult()
}

sealed class ServiceDeleteResult {
    data object Success : ServiceDeleteResult()
    data object NotFound : ServiceDeleteResult()
    data class Failure(val message: String) : ServiceDeleteResult()
}

sealed class CategoryOperationResult {
    data class Success(val category: ServiceCategoryDto) : CategoryOperationResult()
    data object NotFound : CategoryOperationResult()
    data class Failure(val message: String) : CategoryOperationResult()
}

sealed class CategoryDeleteResult {
    data object Success : CategoryDeleteResult()
    data object NotFound : CategoryDeleteResult()
    data class Failure(val message: String) : CategoryDeleteResult()
}

class ServiceRepository(
    private val outboxRepository: OutboxRepository,
    private val json: Json
) : CoreServiceRepository {

    override fun observeCategories(): Flow<Outcome<List<ServiceCategory>>> = flow {
        emit(getCategories())
    }

    override fun observeServices(filter: ServiceFilter): Flow<Outcome<List<Service>>> = flow {
        emit(getServices(filter))
    }

    override suspend fun getCategories(): Outcome<List<ServiceCategory>> {
        return Outcome.Success(transaction {
            ServiceCategoriesTable.selectAll().map { 
                val categoryId = it[ServiceCategoriesTable.id]
                
                ServiceCategory(
                    id = categoryId,
                    name = it[ServiceCategoriesTable.name],
                    iconName = it[ServiceCategoriesTable.iconName],
                    iconUrl = "/v1/api/services/categories/$categoryId/image"
                )
            }
        })
    }

    override suspend fun getServices(filter: ServiceFilter): Outcome<List<Service>> {
        return Outcome.Success(transaction {
            val dbQuery = ServicesTable.selectAll()
            filter.categoryId?.let { categoryId ->
                dbQuery.andWhere { ServicesTable.categoryId eq categoryId }
            }
            filter.searchQuery?.let { query ->
                dbQuery.andWhere { ServicesTable.name.lowerCase() like "%${query.lowercase()}%" }
            }
            filter.minPrice?.let { min ->
                dbQuery.andWhere { ServicesTable.price greaterEq min }
            }
            filter.maxPrice?.let { max ->
                dbQuery.andWhere { ServicesTable.price lessEq max }
            }
            dbQuery.map { it.toService() }
        })
    }

    override suspend fun getService(serviceId: String): Outcome<Service> {
        return transaction {
            ServicesTable.selectAll().where { ServicesTable.id eq serviceId }
                .map { Outcome.Success(it.toService()) }
                .singleOrNull() ?: Outcome.Error(Failure.ServerError(404, "Service not found"))
        }
    }

    override suspend fun submitReview(serviceId: String, rating: Int, comment: String): Outcome<Unit> = transaction {
        try {
            val reviewId = UUID.randomUUID().toString()
            val userId = UsersTable.selectAll().limit(1).map { it[UsersTable.id] }.singleOrNull() ?: "guest"
            
            ReviewsTable.insert {
                it[ReviewsTable.id] = reviewId
                it[ReviewsTable.userId] = userId
                it[ReviewsTable.targetId] = serviceId
                it[ReviewsTable.targetType] = "SERVICE"
                it[ReviewsTable.rating] = rating
                it[ReviewsTable.comment] = comment
                it[ReviewsTable.createdAt] = System.currentTimeMillis()
            }
            
            val user = UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()
            
            val event = DomainEvent.ReviewSubmitted(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                actorId = userId,
                message = "Review submitted for service $serviceId",
                reviewId = reviewId,
                targetId = serviceId,
                targetType = "SERVICE",
                rating = rating,
                userName = user?.get(UsersTable.name),
                userAvatarUrl = user?.get(UsersTable.avatarUrl)
            )
            outboxRepository.save(userId, json.encodeToString(event))
            
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Error(Failure.ServerError(500, e.message ?: "Failed to submit review"))
        }
    }

    fun findAllCategories(): List<ServiceCategoryDto> = transaction {
        ServiceCategoriesTable.selectAll().map { 
            val categoryId = it[ServiceCategoriesTable.id]
            val firstImage = ServicesTable
                .select(ServicesTable.imageUrl)
                .where { ServicesTable.categoryId eq categoryId }
                .limit(1)
                .map { row -> row[ServicesTable.imageUrl] }
                .firstOrNull()

            ServiceCategoryDto(
                id = categoryId,
                name = it[ServiceCategoriesTable.name],
                iconUrl = "/v1/api/services/categories/$categoryId/image"
            )
        }
    }

    fun findNonEmptyCategories(): List<ServiceCategoryDto> = transaction {
        val categoryIdsWithServices = ServicesTable.selectAll()
            .map { it[ServicesTable.categoryId] }
            .distinct()
            
        ServiceCategoriesTable.selectAll()
            .where { ServiceCategoriesTable.id inList categoryIdsWithServices }
            .map { 
                val categoryId = it[ServiceCategoriesTable.id]

                ServiceCategoryDto(
                    id = categoryId,
                    name = it[ServiceCategoriesTable.name],
                    iconUrl = "/v1/api/services/categories/$categoryId/image"
                )
            }
    }

    fun findById(id: String): ServiceFetchResult = transaction {
        ServicesTable.selectAll().where { ServicesTable.id eq id }
            .map { it.toServiceDto() }
            .singleOrNull()?.let { ServiceFetchResult.Success(it) } ?: ServiceFetchResult.NotFound
    }

    fun findAll(categoryId: String?, page: Int, pageSize: Int): List<ServiceDto> = transaction {
        val query = if (categoryId != null) {
            ServicesTable.selectAll().where { ServicesTable.categoryId eq categoryId }
        } else {
            ServicesTable.selectAll()
        }
        
        val items = query.limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map { it.toServiceDto() }
        
        items
    }

    fun create(request: CreateServiceRequestDto): ServiceCreateResult = transaction {
        try {
            val id = UUID.randomUUID().toString()
            ServicesTable.insert {
                it[ServicesTable.id] = id
                it[ServicesTable.name] = request.name
                it[ServicesTable.description] = request.description ?: ""
                it[ServicesTable.price] = request.price
                it[ServicesTable.durationMinutes] = request.durationMinutes
                it[ServicesTable.categoryId] = request.categoryId
                it[ServicesTable.imageUrl] = request.imageUrl
                it[ServicesTable.subCategory] = request.subCategory
                it[ServicesTable.rating] = request.rating ?: 0.0
                it[ServicesTable.createdAt] = System.currentTimeMillis()
            }
            findById(id).let {
                if (it is ServiceFetchResult.Success) ServiceCreateResult.Success(it.service)
                else ServiceCreateResult.Failure("Failed to retrieve created service")
            }
        } catch (e: Exception) {
            ServiceCreateResult.Failure(e.message ?: "Database error")
        }
    }

    fun update(id: String, request: UpdateServiceRequestDto): ServiceUpdateResult = transaction {
        try {
            val updatedRows = ServicesTable.update({ ServicesTable.id eq id }) {
                request.name?.let { name -> it[ServicesTable.name] = name }
                request.description?.let { description -> it[ServicesTable.description] = description }
                request.price?.let { price -> it[ServicesTable.price] = price }
                request.durationMinutes?.let { durationMinutes -> it[ServicesTable.durationMinutes] = durationMinutes }
                request.categoryId?.let { categoryId -> it[ServicesTable.categoryId] = categoryId }
                request.imageUrl?.let { imageUrl -> it[ServicesTable.imageUrl] = imageUrl }
                request.subCategory?.let { subCategory -> it[ServicesTable.subCategory] = subCategory }
                request.rating?.let { rating -> it[ServicesTable.rating] = rating }
                request.isActive?.let { isActive -> it[ServicesTable.isActive] = isActive }
            }
            if (updatedRows > 0) {
                val event = DomainEvent.ServiceUpdated(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    actorId = "admin", // Assuming admin for now
                    message = "Service $id updated",
                    serviceId = id
                )
                outboxRepository.save(null, json.encodeToString(event))
                ServiceUpdateResult.Success
            } else ServiceUpdateResult.NotFound
        } catch (e: Exception) {
            ServiceUpdateResult.Failure(e.message ?: "Database error")
        }
    }

    fun delete(id: String): ServiceDeleteResult = transaction {
        try {
            val deletedRows = ServicesTable.deleteWhere { ServicesTable.id eq id }
            if (deletedRows > 0) {
                val event = DomainEvent.ServiceUpdated(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    actorId = "admin",
                    message = "Service $id deleted",
                    serviceId = id
                )
                outboxRepository.save(null, json.encodeToString(event))
                ServiceDeleteResult.Success
            } else ServiceDeleteResult.NotFound
        } catch (e: Exception) {
            ServiceDeleteResult.Failure(e.message ?: "Database error")
        }
    }

    fun getImagePath(id: String): String? = transaction {
        ServicesTable.select(ServicesTable.imageUrl)
            .where { ServicesTable.id eq id }
            .map { it[ServicesTable.imageUrl] }
            .singleOrNull()
    }

    fun getCategoryImagePath(categoryId: String): String? = transaction {
        ServiceCategoriesTable.select(ServiceCategoriesTable.iconName)
            .where { ServiceCategoriesTable.id eq categoryId }
            .map { it[ServiceCategoriesTable.iconName] }
            .singleOrNull()
    }

    fun findCategoryById(id: String): CategoryOperationResult = transaction {
        ServiceCategoriesTable.selectAll().where { ServiceCategoriesTable.id eq id }
            .map { it.toCategoryDto() }
            .singleOrNull()?.let { CategoryOperationResult.Success(it) } ?: CategoryOperationResult.NotFound
    }

    fun createCategory(name: String, iconName: String?, imageUrl: String?): CategoryOperationResult = transaction {
        try {
            val id = UUID.randomUUID().toString()
            ServiceCategoriesTable.insert {
                it[ServiceCategoriesTable.id] = id
                it[ServiceCategoriesTable.name] = name
                it[ServiceCategoriesTable.iconName] = iconName
                it[ServiceCategoriesTable.imageUrl] = imageUrl
            }
            findCategoryById(id)
        } catch (e: Exception) {
            CategoryOperationResult.Failure(e.message ?: "Database error")
        }
    }

    fun updateCategory(id: String, name: String?, iconName: String?, imageUrl: String?): CategoryOperationResult = transaction {
        try {
            val updatedRows = ServiceCategoriesTable.update({ ServiceCategoriesTable.id eq id }) {
                name?.let { n -> it[ServiceCategoriesTable.name] = n }
                iconName?.let { i -> it[ServiceCategoriesTable.iconName] = i }
                imageUrl?.let { img -> it[ServiceCategoriesTable.imageUrl] = img }
            }
            if (updatedRows > 0) findCategoryById(id)
            else CategoryOperationResult.NotFound
        } catch (e: Exception) {
            CategoryOperationResult.Failure(e.message ?: "Database error")
        }
    }

    fun deleteCategory(id: String): CategoryDeleteResult = transaction {
        try {
            // Check if there are services in this category
            val hasServices = ServicesTable.selectAll().where { ServicesTable.categoryId eq id }.any()
            if (hasServices) {
                return@transaction CategoryDeleteResult.Failure("Cannot delete category with associated services")
            }
            
            val deletedRows = ServiceCategoriesTable.deleteWhere { ServiceCategoriesTable.id eq id }
            if (deletedRows > 0) CategoryDeleteResult.Success
            else CategoryDeleteResult.NotFound
        } catch (e: Exception) {
            CategoryDeleteResult.Failure(e.message ?: "Database error")
        }
    }

    private fun ResultRow.toCategoryDto() = ServiceCategoryDto(
        id = this[ServiceCategoriesTable.id],
        name = this[ServiceCategoriesTable.name],
        iconUrl = this[ServiceCategoriesTable.imageUrl] ?: this[ServiceCategoriesTable.iconName]?.let { "/v1/api/services/categories/${this[ServiceCategoriesTable.id]}/image" }
    )

    private fun ResultRow.toService() = transaction {
        val id = this@toService[ServicesTable.id]
        
        val directReviews = ReviewsTable.join(UsersTable, JoinType.INNER, ReviewsTable.userId, UsersTable.id)
            .selectAll().where { 
                (ReviewsTable.targetId eq id) and 
                (ReviewsTable.targetType eq "SERVICE") and
                (ReviewsTable.isVisible eq true)
            }
            .map { it.toDomainReview() }

        val appointmentReviews = ReviewsTable.join(UsersTable, JoinType.INNER, ReviewsTable.userId, UsersTable.id)
            .join(AppointmentsTable, JoinType.INNER, ReviewsTable.targetId, AppointmentsTable.id)
            .selectAll().where {
                (ReviewsTable.targetType eq "APPOINTMENT") and
                (AppointmentsTable.servicesJson like "%$id%") and
                (ReviewsTable.isVisible eq true)
            }
            .map { it.toDomainReview() }

        val reviews = (directReviews + appointmentReviews).sortedByDescending { it.createdAtEpochSeconds }
        val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average() else 0.0

        Service(
            id = id,
            name = this@toService[ServicesTable.name],
            description = this@toService[ServicesTable.description],
            durationMinutes = this@toService[ServicesTable.durationMinutes],
            price = this@toService[ServicesTable.price],
            categoryId = this@toService[ServicesTable.categoryId],
            imageUrl = this@toService[ServicesTable.imageUrl],
            reviews = reviews,
            rating = avgRating
        )
    }

    private fun ResultRow.toDomainReview() = iz.mkao.mirasalon.core.domain.model.Review(
        id = this[ReviewsTable.id],
        userName = this[UsersTable.name],
        userAvatarUrl = this[UsersTable.avatarUrl],
        rating = this[ReviewsTable.rating],
        comment = this[ReviewsTable.comment],
        createdAtEpochSeconds = this[ReviewsTable.createdAt] / 1000
    )

    private fun ResultRow.toServiceDto() = transaction {
        val id = this@toServiceDto[ServicesTable.id]
        
        val directReviews = ReviewsTable.join(UsersTable, JoinType.INNER, ReviewsTable.userId, UsersTable.id)
            .selectAll().where { 
                (ReviewsTable.targetId eq id) and 
                (ReviewsTable.targetType eq "SERVICE") and
                (ReviewsTable.isVisible eq true)
            }
            .map { it.toReviewDto() }

        val appointmentReviews = ReviewsTable.join(UsersTable, JoinType.INNER, ReviewsTable.userId, UsersTable.id)
            .join(AppointmentsTable, JoinType.INNER, ReviewsTable.targetId, AppointmentsTable.id)
            .selectAll().where {
                (ReviewsTable.targetType eq "APPOINTMENT") and
                (AppointmentsTable.servicesJson like "%$id%") and
                (ReviewsTable.isVisible eq true)
            }
            .map { it.toReviewDto() }

        val reviews = (directReviews + appointmentReviews).sortedByDescending { it.createdAtEpochSeconds }
        val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average() else 0.0

        ServiceDto(
            id = id,
            name = this@toServiceDto[ServicesTable.name],
            description = this@toServiceDto[ServicesTable.description],
            price = this@toServiceDto[ServicesTable.price],
            durationMinutes = this@toServiceDto[ServicesTable.durationMinutes],
            imageUrl = if (this@toServiceDto[ServicesTable.imageUrl] != null) "/v1/api/services/${this@toServiceDto[ServicesTable.id]}/image" else null,
            categoryId = this@toServiceDto[ServicesTable.categoryId],
            subCategory = this@toServiceDto[ServicesTable.subCategory],
            rating = avgRating,
            reviews = reviews,
            isActive = this@toServiceDto[ServicesTable.isActive]
        )
    }

    private fun ResultRow.toReviewDto() = ReviewDto(
        id = this[ReviewsTable.id],
        userName = this[UsersTable.name],
        userAvatarUrl = this[UsersTable.avatarUrl],
        rating = this[ReviewsTable.rating],
        comment = this[ReviewsTable.comment],
        createdAtEpochSeconds = this[ReviewsTable.createdAt] / 1000,
        targetId = this[ReviewsTable.targetId],
        targetType = this[ReviewsTable.targetType]
    )
}
