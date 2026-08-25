package iz.mkao.mirasalon.server.data.repository

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.domain.model.ServiceFilter
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.CreateServiceRequestDto
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceCategoryDto
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateServiceRequestDto
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import iz.mkao.mirasalon.server.data.tables.AppointmentsTable
import iz.mkao.mirasalon.server.data.tables.PromotionsTable
import iz.mkao.mirasalon.server.data.tables.ReviewsTable
import iz.mkao.mirasalon.server.data.tables.ServiceCategoriesTable
import iz.mkao.mirasalon.server.data.tables.ServicesTable
import iz.mkao.mirasalon.server.data.tables.UsersTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    private val outboxRepository: OutboxRepository
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
            dbQuery.orderBy(ServicesTable.createdAt to SortOrder.DESC)
                .map { it.toService() }
        })
    }

    override suspend fun getService(serviceId: String): Outcome<Service> {
        return transaction {
            ServicesTable.selectAll().where { ServicesTable.id eq serviceId }
                .map { Outcome.Success(it.toService()) }
                .singleOrNull() ?: Outcome.Error(Failure.ServerError(404, "Service not found"))
        }
    }

    override suspend fun submitReview(serviceId: String, rating: Int, comment: String, userId: String?): Outcome<Unit> = transaction {
        try {
            val finalUserId = userId ?: return@transaction Outcome.Error(Failure.ClientError(401, "Authentication required"))

            // Check if user has had a completed appointment for this service
            val appointmentCount = AppointmentsTable.selectAll().where {
                (AppointmentsTable.userId eq finalUserId) and
                (AppointmentsTable.servicesJson like "%$serviceId%") and
                (AppointmentsTable.status eq "COMPLETED")
            }.count()

            if (appointmentCount == 0L) {
                return@transaction Outcome.Error(Failure.ServerError(403, "You can only review services you have already completed."))
            }

            // Check for existing review
            val reviewCount = ReviewsTable.selectAll().where {
                (ReviewsTable.userId eq finalUserId) and
                (ReviewsTable.targetId eq serviceId) and
                (ReviewsTable.targetType eq "SERVICE")
            }.count()

            if (reviewCount >= appointmentCount) {
                return@transaction Outcome.Error(Failure.ServerError(409, "You have already submitted reviews for all your recent completions of this service."))
            }

            ReviewsTable.insert {
                it[ReviewsTable.id] = UUID.randomUUID().toString()
                it[ReviewsTable.userId] = finalUserId
                it[ReviewsTable.targetId] = serviceId
                it[ReviewsTable.targetType] = "SERVICE"
                it[ReviewsTable.rating] = rating
                it[ReviewsTable.comment] = comment
                it[ReviewsTable.createdAt] = System.currentTimeMillis()
            }

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
        
        val items = query
            .orderBy(ServicesTable.createdAt to SortOrder.DESC)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
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
                outboxRepository.save(null, DomainEventCodec.encode(event))
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
                outboxRepository.save(null, DomainEventCodec.encode(event))
                ServiceDeleteResult.Success
            } else ServiceDeleteResult.NotFound
        } catch (e: Exception) {
            ServiceDeleteResult.Failure(e.message ?: "Database error")
        }
    }

    fun getImagePath(id: String): String? = transaction {
        val result = ServicesTable.select(ServicesTable.name, ServicesTable.imageUrl)
            .where { ServicesTable.id eq id }
            .map { it[ServicesTable.name] to it[ServicesTable.imageUrl] }
            .singleOrNull()
            
        result?.second ?: ApiEndpoints.getServicePlaceholder(result?.first)
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
        val categoryId = this@toService[ServicesTable.categoryId]

        val categoryName = ServiceCategoriesTable.select(ServiceCategoriesTable.name)
            .where { ServiceCategoriesTable.id eq categoryId }
            .map { it[ServiceCategoriesTable.name] }
            .singleOrNull() ?: ""

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

        // Calculate automatic discount from category
        val autoDiscountPercent = getBestAutomaticDiscountPercent(categoryName, this@toService[ServicesTable.price])

        Service(
            id = id,
            name = this@toService[ServicesTable.name],
            description = this@toService[ServicesTable.description],
            durationMinutes = this@toService[ServicesTable.durationMinutes],
            price = this@toService[ServicesTable.price],
            categoryId = categoryId,
            discountPercent = autoDiscountPercent,
            imageUrl = this@toService[ServicesTable.imageUrl] ?: ApiEndpoints.getServicePlaceholder(this@toService[ServicesTable.name]),
            reviews = reviews,
            rating = avgRating
        )
    }

    private fun getBestAutomaticDiscountPercent(categoryName: String, itemPrice: Double): Int {
        if (categoryName.isBlank()) return 0
        val now = System.currentTimeMillis()
        return transaction {
            PromotionsTable.selectAll().where {
                (PromotionsTable.status eq "ACTIVE") and
                // Allow "welcome" promo to be automatic if the user wants it to be "auto"
                (PromotionsTable.code.isNull() or (PromotionsTable.code eq "welcome")) and
                (PromotionsTable.discountType eq "PERCENTAGE") and
                (PromotionsTable.validFrom.isNull() or (PromotionsTable.validFrom lessEq now)) and
                (PromotionsTable.validUntil.isNull() or (PromotionsTable.validUntil greaterEq now))
            }.mapNotNull { row ->
                val categories = row[PromotionsTable.applicableCategories]
                    ?.split(",")
                    ?.map { it.trim().lowercase() } ?: emptyList()
                val minVal = row[PromotionsTable.minOrderValue] ?: 0.0
                
                if (categoryName.trim().lowercase() in categories && itemPrice >= minVal) {
                    row[PromotionsTable.discountValue].toInt()
                } else null
            }.maxOrNull() ?: 0
        }
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
        val categoryId = this@toServiceDto[ServicesTable.categoryId]

        val categoryName = ServiceCategoriesTable.select(ServiceCategoriesTable.name)
            .where { ServiceCategoriesTable.id eq categoryId }
            .map { it[ServiceCategoriesTable.name] }
            .singleOrNull() ?: ""

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

        val autoDiscountPercent = getBestAutomaticDiscountPercent(categoryName, this@toServiceDto[ServicesTable.price])

        ServiceDto(
            id = id,
            name = this@toServiceDto[ServicesTable.name],
            description = this@toServiceDto[ServicesTable.description],
            price = this@toServiceDto[ServicesTable.price],
            durationMinutes = this@toServiceDto[ServicesTable.durationMinutes],
            imageUrl = "/v1/api/services/${this@toServiceDto[ServicesTable.id]}/image",
            categoryId = categoryId,
            subCategory = this@toServiceDto[ServicesTable.subCategory],
            discountPercent = autoDiscountPercent,
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
