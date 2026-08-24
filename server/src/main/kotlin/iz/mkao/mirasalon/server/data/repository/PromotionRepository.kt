package iz.mkao.mirasalon.server.data.repository

import iz.mkao.mirasalon.core.domain.model.CartItem
import iz.mkao.mirasalon.core.domain.model.DiscountType
import iz.mkao.mirasalon.core.domain.model.PromoStatus
import iz.mkao.mirasalon.core.domain.model.PromoType
import iz.mkao.mirasalon.core.domain.model.PromoValidation
import iz.mkao.mirasalon.core.domain.model.Promotion
import iz.mkao.mirasalon.core.domain.model.UsageLimit
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.util.PromotionValidator
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.CreatePromotionRequestDto
import iz.mkao.mirasalon.core.network.model.dto.UpdatePromotionRequestDto
import iz.mkao.mirasalon.server.data.tables.ProductsTable
import iz.mkao.mirasalon.server.data.tables.PromotionUsagesTable
import iz.mkao.mirasalon.server.data.tables.PromotionsTable
import iz.mkao.mirasalon.server.data.tables.ServicesTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.util.UUID
import kotlin.time.Instant
import iz.mkao.mirasalon.core.domain.repository.PromoRepository as CorePromoRepository


class PromotionRepository(
    private val outboxRepository: OutboxRepository,
    private val json: Json,
    private val clock: Clock
) : CorePromoRepository {

    override suspend fun validatePromo(code: String, cartItems: List<CartItem>): Outcome<PromoValidation> = try {
        newSuspendedTransaction {
            val promoRow = PromotionsTable.selectAll().where { PromotionsTable.code eq code }
                .singleOrNull()

            if (promoRow == null) {
                return@newSuspendedTransaction Outcome.Success(
                    PromoValidation(
                        promoCode = code,
                        isValid = false,
                        errorMessage = "Invalid promo code"
                    )
                )
            }

            val promo = promoRow.toDomain()
            val total = cartItems.sumOf { it.product.price * it.quantity }
            val serviceIds = cartItems.map { it.product.id }
            val categoryIds = cartItems.map { it.product.category }

            val result = PromotionValidator.validate(
                promo = promo,
                cartTotal = total,
                userId = null,
                serviceIds = serviceIds,
                categoryIds = categoryIds,
                now = Instant.fromEpochMilliseconds(clock.millis())
            )

            Outcome.Success(
                PromoValidation(
                    promoCode = code,
                    isValid = result.isValid,
                    discountAmount = result.discountAmount,
                    applicableServices = result.applicableServices,
                    applicableCategories = result.applicableCategories,
                    errorMessage = result.errorMessage
                )
            )
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Validation failed: ${e.message}"))
    }

    override fun observePromotions(): Flow<Outcome<List<Promotion>>> = flow {
        emit(fetchPromotions())
    }

    override suspend fun fetchPromotions(): Outcome<List<Promotion>> = try {
        newSuspendedTransaction {
            Outcome.Success(PromotionsTable.selectAll()
                .orderBy(PromotionsTable.createdAt to SortOrder.DESC)
                .map { it.toDomain() })
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to fetch promotions: ${e.message}"))
    }

    suspend fun findActive(page: Int, pageSize: Int, userId: String?): Outcome<List<Promotion>> = try {
        newSuspendedTransaction {
            val now = clock.millis()
            val query = PromotionsTable.selectAll().where {
                (PromotionsTable.status eq "ACTIVE") and
                        (PromotionsTable.validFrom.isNull() or (PromotionsTable.validFrom lessEq now)) and
                        (PromotionsTable.validUntil.isNull() or (PromotionsTable.validUntil greaterEq now))
            }

            if (userId != null) {
                query.andWhere { PromotionsTable.targetUserId.isNull() or (PromotionsTable.targetUserId eq userId) }
            } else {
                query.andWhere { PromotionsTable.targetUserId.isNull() }
            }

            val items = query
                .orderBy(PromotionsTable.createdAt to SortOrder.DESC)
                .limit(pageSize).offset(((page - 1) * pageSize).toLong())
                .map { it.toDomain() }

            Outcome.Success(items)
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to fetch active promotions: ${e.message}"))
    }

    suspend fun findAll(page: Int, pageSize: Int, query: String? = null): Outcome<PagedResponse<Promotion>> = try {
        newSuspendedTransaction {
            val baseQuery = PromotionsTable.selectAll()

            if (query != null) {
                val searchTerm = "%${query.lowercase()}%"
                baseQuery.andWhere {
                    (PromotionsTable.code.lowerCase() like searchTerm) or
                            (PromotionsTable.description.lowerCase() like searchTerm)
                }
            }

            val total = baseQuery.count()
            val items = baseQuery
                .orderBy(PromotionsTable.createdAt to SortOrder.DESC)
                .limit(pageSize).offset(((page - 1) * pageSize).toLong())
                .map { it.toDomain() }

            val totalPages = if (pageSize > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
            Outcome.Success(PagedResponse(items, total, page, pageSize, totalPages))
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to search promotions: ${e.message}"))
    }

    suspend fun findById(id: String): Outcome<Promotion> = try {
        newSuspendedTransaction {
            PromotionsTable.selectAll().where { PromotionsTable.id eq id }
                .map { it.toDomain() }
                .singleOrNull()?.let { Outcome.Success(it) }
                ?: Outcome.Error(Failure.ClientError(404, "Promotion not found"))
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to find promotion: ${e.message}"))
    }

    suspend fun create(request: CreatePromotionRequestDto, actorId: String? = null): Outcome<Promotion> = try {
        newSuspendedTransaction {
            val id = UUID.randomUUID().toString()
            PromotionsTable.insert {
                it[PromotionsTable.id] = id
                it[PromotionsTable.code] = request.code
                it[PromotionsTable.title] = request.title
                it[PromotionsTable.ctaText] = request.ctaText
                it[PromotionsTable.description] = request.description
                it[PromotionsTable.discountType] = request.discountType
                it[PromotionsTable.discountValue] = request.discountValue
                it[PromotionsTable.validFrom] = request.validFrom
                it[PromotionsTable.validUntil] = request.validUntil
                it[PromotionsTable.totalRedemptions] = request.totalRedemptions
                it[PromotionsTable.perUserRedemptions] = request.perUserRedemptions
                it[PromotionsTable.minOrderValue] = request.minOrderValue
                it[PromotionsTable.applicableServices] = request.applicableServices?.joinToString(",")
                it[PromotionsTable.applicableCategories] = request.applicableCategories?.joinToString(",")
                it[PromotionsTable.targetUserId] = request.targetUserId
                it[PromotionsTable.isFirstPurchaseOnly] = request.isFirstPurchaseOnly
                it[PromotionsTable.stackable] = request.stackable
                it[PromotionsTable.status] = request.status
                it[PromotionsTable.imageUrl] = request.imageUrl
                it[PromotionsTable.promoType] = request.type ?: "EXPERTS"
                it[PromotionsTable.createdAt] = clock.millis()
            }

            val result = findById(id)
            if (result is Outcome.Success) {
                broadcastChange(id, actorId ?: "system")
                result
            } else {
                Outcome.Error(Failure.ServerError(500, "Failed to retrieve created promotion"))
            }
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to create promotion: ${e.message}"))
    }

    suspend fun update(id: String, request: UpdatePromotionRequestDto, actorId: String? = null): Outcome<Unit> = try {
        newSuspendedTransaction {
            val updatedRows = PromotionsTable.update({ PromotionsTable.id eq id }) {
                request.code?.let { code -> it[PromotionsTable.code] = code }
                request.title?.let { title -> it[PromotionsTable.title] = title }
                request.ctaText?.let { ctaText -> it[PromotionsTable.ctaText] = ctaText }
                request.description?.let { description -> it[PromotionsTable.description] = description }
                request.discountType?.let { discountType -> it[PromotionsTable.discountType] = discountType }
                request.discountValue?.let { discountValue -> it[PromotionsTable.discountValue] = discountValue }
                request.validFrom?.let { validFrom -> it[PromotionsTable.validFrom] = validFrom }
                request.validUntil?.let { validUntil -> it[PromotionsTable.validUntil] = validUntil }
                request.totalRedemptions?.let { totalRedemptions -> it[PromotionsTable.totalRedemptions] = totalRedemptions }
                request.perUserRedemptions?.let { perUserRedemptions -> it[PromotionsTable.perUserRedemptions] = perUserRedemptions }
                request.minOrderValue?.let { minOrderValue -> it[PromotionsTable.minOrderValue] = minOrderValue }
                request.applicableServices?.let { applicableServices ->
                    it[PromotionsTable.applicableServices] = applicableServices.joinToString(",")
                }
                request.applicableCategories?.let { applicableCategories ->
                    it[PromotionsTable.applicableCategories] = applicableCategories.joinToString(",")
                }
                request.targetUserId?.let { targetUserId -> it[PromotionsTable.targetUserId] = targetUserId }
                request.isFirstPurchaseOnly?.let { isFirstPurchaseOnly -> it[PromotionsTable.isFirstPurchaseOnly] = isFirstPurchaseOnly }
                request.stackable?.let { stackable -> it[PromotionsTable.stackable] = stackable }
                request.status?.let { status -> it[PromotionsTable.status] = status }
                request.imageUrl?.let { imageUrl -> it[PromotionsTable.imageUrl] = imageUrl }
                request.type?.let { type -> it[PromotionsTable.promoType] = type }
            }
            if (updatedRows > 0) {
                broadcastChange(id, actorId ?: "system")
                Outcome.Success(Unit)
            } else Outcome.Error(Failure.ClientError(404, "Promotion not found"))
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to update promotion: ${e.message}"))
    }

    suspend fun delete(id: String, actorId: String? = null): Outcome<Unit> = try {
        newSuspendedTransaction {
            val deletedRows = PromotionsTable.deleteWhere { PromotionsTable.id eq id }
            if (deletedRows > 0) {
                broadcastChange(id, actorId ?: "system")
                Outcome.Success(Unit)
            } else Outcome.Error(Failure.ClientError(404, "Promotion not found"))
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to delete promotion: ${e.message}"))
    }

    private fun broadcastChange(promotionId: String, actorId: String) {
        val event = DomainEvent.PromotionChanged(
            eventId = UUID.randomUUID().toString(),
            timestamp = clock.millis(),
            actorId = actorId,
            message = "Promotion changed",
            promotionId = promotionId
        )
        outboxRepository.save(null, json.encodeToString(event))
    }

    suspend fun getImagePath(id: String): Outcome<String?> = try {
        newSuspendedTransaction {
            val path = PromotionsTable.select(PromotionsTable.imageUrl)
                .where { PromotionsTable.id eq id }
                .map { it[PromotionsTable.imageUrl] }
                .singleOrNull()
            Outcome.Success(path)
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to get image path: ${e.message}"))
    }

    suspend fun getUserUsedPromotionIds(userId: String): Outcome<List<String>> = try {
        newSuspendedTransaction {
            val ids = PromotionUsagesTable.selectAll()
                .where { PromotionUsagesTable.userId eq userId }
                .map { it[PromotionUsagesTable.promotionId] }
            Outcome.Success(ids)
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to fetch used promotions: ${e.message}"))
    }

    override suspend fun getUsedPromotionIds(): Outcome<List<String>> = try {
        newSuspendedTransaction {
            val ids = PromotionUsagesTable.selectAll()
                .map { it[PromotionUsagesTable.promotionId] }
                .distinct()
            Outcome.Success(ids)
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to fetch used promotion IDs: ${e.message}"))
    }

    suspend fun deleteAll(): Outcome<Int> = try {
        newSuspendedTransaction {
            Outcome.Success(PromotionsTable.deleteAll())
        }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to delete all promotions: ${e.message}"))
    }

    suspend fun getUserPromoUsageCount(promotionId: String, userId: String): Int {
        val tx = TransactionManager.currentOrNull()
        return tx?.withSuspendTransaction {
            getUserPromoUsageCountInternal(promotionId, userId)
        }
            ?: newSuspendedTransaction {
                getUserPromoUsageCountInternal(promotionId, userId)
            }
    }

    private fun Transaction.getUserPromoUsageCountInternal(promotionId: String, userId: String): Int {
        return PromotionUsagesTable.selectAll()
            .where { (PromotionUsagesTable.promotionId eq promotionId) and (PromotionUsagesTable.userId eq userId) }
            .count()
            .toInt()
    }

    suspend fun recordPromoUsage(promotionId: String, userId: String, orderId: String?): Outcome<Unit> = try {
        val tx = TransactionManager.currentOrNull()
        if (tx != null) {
            tx.withSuspendTransaction {
                recordPromoUsageInternal(promotionId, userId, orderId)
            }
        } else {
            newSuspendedTransaction {
                recordPromoUsageInternal(promotionId, userId, orderId)
            }
        }
        Outcome.Success(Unit)
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Failed to record promo usage: ${e.message}"))
    }

    private fun recordPromoUsageInternal(promotionId: String, userId: String, orderId: String?) {
        PromotionUsagesTable.insert {
            it[PromotionUsagesTable.id] = UUID.randomUUID().toString()
            it[PromotionUsagesTable.promotionId] = promotionId
            it[PromotionUsagesTable.userId] = userId
            it[PromotionUsagesTable.orderId] = orderId
            it[PromotionUsagesTable.createdAt] = clock.millis()
        }

        PromotionsTable.update({ PromotionsTable.id eq promotionId }) {
            it[currentUsageCount] = PromotionsTable.currentUsageCount.plus(1)
        }
    }

    suspend fun validatePromoCode(
        code: String,
        userId: String,
        cartTotal: Double,
        serviceIds: List<String>? = null,
        categoryIds: List<String>? = null
    ): Outcome<PromoValidation> = try {
        val tx = TransactionManager.currentOrNull()
        tx?.withSuspendTransaction {
            validatePromoCodeInternal(code, userId, cartTotal, serviceIds, categoryIds)
        }
            ?: newSuspendedTransaction {
                validatePromoCodeInternal(code, userId, cartTotal, serviceIds, categoryIds)
            }
    } catch (e: Exception) {
        Outcome.Error(Failure.ServerError(500, "Validation failed: ${e.message}"))
    }

    private suspend fun validatePromoCodeInternal(
        code: String,
        userId: String,
        cartTotal: Double,
        serviceIds: List<String>?,
        categoryIds: List<String>?
    ): Outcome<PromoValidation> {
        val promoRow = PromotionsTable.selectAll().where { PromotionsTable.code eq code }.singleOrNull()
            ?: return Outcome.Error(Failure.ClientError(404, "Promo code not found"))

        val promo = promoRow.toDomain()
        
        // Resolve categories if not provided (lookup by service/product IDs)
        val resolvedCategoryIds = categoryIds?.toMutableList() ?: mutableListOf()
        if (!serviceIds.isNullOrEmpty()) {
            val serviceCats = ServicesTable
                .select(ServicesTable.categoryId)
                .where { ServicesTable.id inList serviceIds }
                .map { it[ServicesTable.categoryId] }
            resolvedCategoryIds.addAll(serviceCats)

            val productCats = ProductsTable
                .select(ProductsTable.category)
                .where { ProductsTable.id inList serviceIds }
                .map { it[ProductsTable.category] }
            resolvedCategoryIds.addAll(productCats)
        }
        
        val userUsageCount = getUserPromoUsageCount(promoRow[PromotionsTable.id], userId)

        val result = PromotionValidator.validate(
            promo = promo,
            cartTotal = cartTotal,
            userId = userId,
            serviceIds = serviceIds,
            categoryIds = resolvedCategoryIds.distinct(),
            userUsageCount = userUsageCount,
            now = Instant.fromEpochMilliseconds(clock.millis())
        )

        return Outcome.Success(
            PromoValidation(
                promoCode = code,
                isValid = result.isValid,
                discountAmount = result.discountAmount,
                applicableServices = result.applicableServices,
                applicableCategories = result.applicableCategories,
                errorMessage = result.errorMessage
            )
        )
    }

    private fun ResultRow.toDomain() = Promotion(
        id = this[PromotionsTable.id],
        code = this[PromotionsTable.code],
        title = this[PromotionsTable.title],
        ctaText = this[PromotionsTable.ctaText],
        description = this[PromotionsTable.description],
        discountType = DiscountType.valueOf(this[PromotionsTable.discountType]),
        discountValue = this[PromotionsTable.discountValue],
        validFrom = this[PromotionsTable.validFrom]?.let { Instant.fromEpochMilliseconds(it) },
        validUntil = this[PromotionsTable.validUntil]?.let { Instant.fromEpochMilliseconds(it) },
        usageLimit = UsageLimit(
            totalRedemptions = this[PromotionsTable.totalRedemptions],
            perUserRedemptions = this[PromotionsTable.perUserRedemptions]
        ),
        currentUsageCount = this[PromotionsTable.currentUsageCount],
        minOrderValue = this[PromotionsTable.minOrderValue],
        applicableServices = this[PromotionsTable.applicableServices]?.split(","),
        applicableCategories = this[PromotionsTable.applicableCategories]?.split(","),
        targetUserId = this[PromotionsTable.targetUserId],
        isFirstPurchaseOnly = this[PromotionsTable.isFirstPurchaseOnly],
        stackable = this[PromotionsTable.stackable],
        status = PromoStatus.valueOf(this[PromotionsTable.status]),
        imageUrl = this[PromotionsTable.imageUrl],
        discountPercent = this[PromotionsTable.discountPercent],
        isActive = this[PromotionsTable.isActive],
        type = try { PromoType.valueOf(this[PromotionsTable.promoType]) } catch(e: Exception) { PromoType.EXPERTS }
    )
}
