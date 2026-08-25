package iz.mkao.mirasalon.server.data.repository

import io.micrometer.core.instrument.MeterRegistry
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.ProductPage
import iz.mkao.mirasalon.core.domain.model.Review
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.CategoryDto
import iz.mkao.mirasalon.core.network.model.dto.CreateProductRequest
import iz.mkao.mirasalon.core.network.model.dto.ProductDto
import iz.mkao.mirasalon.core.network.model.dto.ProductPageDto
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateProductRequest
import iz.mkao.mirasalon.server.data.tables.OrderItemsTable
import iz.mkao.mirasalon.server.data.tables.OrdersTable
import iz.mkao.mirasalon.server.data.tables.OutboxAudience
import iz.mkao.mirasalon.server.data.tables.ProductCategoriesTable
import iz.mkao.mirasalon.server.data.tables.ProductsTable
import iz.mkao.mirasalon.server.data.tables.PromotionsTable
import iz.mkao.mirasalon.server.data.tables.ReviewsTable
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
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.util.UUID
import iz.mkao.mirasalon.core.domain.repository.ProductRepository as CoreProductRepository

sealed class ProductOperationResult {
    data class Success(val product: ProductDto) : ProductOperationResult()
    data object NotFound : ProductOperationResult()
    data class FailureMsg(val message: String) : ProductOperationResult()
}

sealed class ProductDeleteResult {
    data object Success : ProductDeleteResult()
    data object NotFound : ProductDeleteResult()
    data class FailureMsg(val message: String) : ProductDeleteResult()
}

sealed class ProductCategoryOperationResult {
    data class Success(val category: CategoryDto) : ProductCategoryOperationResult()
    data object NotFound : ProductCategoryOperationResult()
    data class FailureMsg(val message: String) : ProductCategoryOperationResult()
}

sealed class ProductCategoryDeleteResult {
    data object Success : ProductCategoryDeleteResult()
    data object NotFound : ProductCategoryDeleteResult()
    data class FailureMsg(val message: String) : ProductCategoryDeleteResult()
}

class ProductRepository(
    private val outboxRepository: OutboxRepository,
    private val meterRegistry: MeterRegistry
) : CoreProductRepository {

    fun registerMetrics() {
        meterRegistry.gauge("products_low_stock_count", this) {
            transaction {
                try {
                    ProductsTable.selectAll().where { 
                        (ProductsTable.stockQuantity less 5) and 
                        (ProductsTable.name.lowerCase() notLike "%test%") 
                    }.count().toDouble()
                } catch (e: Exception) {
                    0.0
                }
            }
        }
    }

    override suspend fun getCategories(): Outcome<List<ProductCategory>> {
        return Outcome.Success(transaction {
            ProductCategoriesTable.selectAll().map { 
                val categoryId = it[ProductCategoriesTable.id]
                val categoryName = it[ProductCategoriesTable.name]
                val count = ProductsTable.selectAll().where { ProductsTable.category eq categoryName }.count().toInt()
                
                ProductCategory(
                    id = categoryId,
                    name = categoryName,
                    imageUrl = it[ProductCategoriesTable.imageUrl]?.takeIf { it.isNotBlank() } ?: "",
                    productCount = count
                )
            }
        })
    }

    override suspend fun getProducts(
        category: String?,
        subCategory: String?,
        page: Int,
        pageSize: Int,
        query: String?,
        gender: String?
    ): Outcome<ProductPage> {
        return Outcome.Success(transaction {
            val baseQuery = ProductsTable.selectAll()
            if (category != null) {
                baseQuery.andWhere { ProductsTable.category eq category }
            }
            if (subCategory != null) {
                baseQuery.andWhere { ProductsTable.subCategory eq subCategory }
            }
            if (gender != null) {
                baseQuery.andWhere { ProductsTable.gender eq gender }
            }

            query?.let { q ->
                val searchTerm = "%${q.lowercase()}%"
                baseQuery.andWhere {
                    (ProductsTable.name.lowerCase() like searchTerm) or
                        (ProductsTable.description.lowerCase() like searchTerm) or
                        (ProductsTable.category.lowerCase() like searchTerm)
                }
            }

            val total = baseQuery.count()
            val products = baseQuery.limit(pageSize).offset(((page - 1) * pageSize).toLong())
                .map { it.toProduct() }
            
            val subCategories = if (category != null) {
                ProductsTable.selectAll().where { ProductsTable.category eq category }
                    .mapNotNull { it[ProductsTable.subCategory] }
                    .distinct()
            } else {
                ProductsTable.selectAll()
                    .mapNotNull { it[ProductsTable.subCategory] }
                    .distinct()
            }
            
            ProductPage(
                products = products,
                subCategories = subCategories,
                hasMore = ((page - 1) * pageSize) + products.size < total
            )
        })
    }

    fun count(): Long = transaction {
        ProductsTable.selectAll().count()
    }

    override fun observeProducts(
        category: String?,
        subCategory: String?,
        page: Int,
        pageSize: Int,
        query: String?,
        gender: String?
    ): Flow<Outcome<ProductPage>> = flow {
        emit(getProducts(category, subCategory, page, pageSize, query, gender))
    }

    override suspend fun getReviews(productId: String): Outcome<List<Review>> {
        return Outcome.Success(transaction {
            ReviewsTable.join(UsersTable, JoinType.INNER, ReviewsTable.userId, UsersTable.id)
                .selectAll()
                .where { 
                    (ReviewsTable.targetId eq productId) and 
                    (ReviewsTable.targetType eq "PRODUCT") and
                    (ReviewsTable.isVisible eq true)
                }
                .map { it.toReview() }
        })
    }

    override suspend fun submitReview(productId: String, rating: Int, comment: String, userId: String?): Outcome<Review> {
        return transaction {
            try {
                val finalUserId = userId ?: return@transaction Outcome.Error(Failure.ClientError(401, "Authentication required"))

                // Business Rule: Check eligibility and allow repeat reviews for repeat purchases (Amazon-style)
                val twoMonthsAgo = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)
                // Count total separate purchases of this product in delivered orders within the last 2 months
                val purchaseCount = (OrdersTable innerJoin OrderItemsTable).selectAll().where {
                    (OrdersTable.userId eq finalUserId) and
                    (OrdersTable.status eq "DELIVERED") and
                    (OrderItemsTable.productId eq productId) and
                    (OrdersTable.createdAt greaterEq twoMonthsAgo)
                }.count()

                if (purchaseCount == 0L) {
                    return@transaction Outcome.Error(Failure.ServerError(403, "You can only review products you have purchased and received within the last 2 months."))
                }

                // Count existing reviews for this product for these purchases
                val reviewCount = ReviewsTable.selectAll().where {
                    (ReviewsTable.userId eq finalUserId) and (ReviewsTable.targetId eq productId) and (ReviewsTable.targetType eq "PRODUCT") and
                    (ReviewsTable.createdAt greaterEq twoMonthsAgo)
                }.count()

                if (reviewCount >= purchaseCount) {
                    return@transaction Outcome.Error(Failure.ServerError(409, "You have already submitted reviews for all your recent purchases of this product."))
                }

                val id = UUID.randomUUID().toString()
                val user = UsersTable.selectAll().where { UsersTable.id eq finalUserId }.singleOrNull()
            
                ReviewsTable.insert {
                    it[ReviewsTable.id] = id
                    it[ReviewsTable.userId] = finalUserId
                    it[ReviewsTable.targetId] = productId
                    it[ReviewsTable.targetType] = "PRODUCT"
                    it[ReviewsTable.rating] = rating
                    it[ReviewsTable.comment] = comment
                    it[ReviewsTable.createdAt] = System.currentTimeMillis()
                }
            
                val review = ReviewsTable.join(UsersTable, JoinType.INNER, ReviewsTable.userId, UsersTable.id)
                    .selectAll().where { ReviewsTable.id eq id }
                    .map { it.toReview() }
                    .singleOrNull()
            
                if (review != null) {
                    val event = DomainEvent.ReviewSubmitted(
                        eventId = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        actorId = finalUserId,
                        message = "Review submitted for product $productId",
                        reviewId = id,
                        targetId = productId,
                        targetType = "PRODUCT",
                        rating = rating,
                        userName = user?.get(UsersTable.name),
                        userAvatarUrl = user?.get(UsersTable.avatarUrl)
                    )
                    // Reviews are back-office signals - route to the admin desktop
                    // dashboard only, never to the client's own notification feed.
                    outboxRepository.save(null, DomainEventCodec.encode(event), OutboxAudience.ADMIN)
                    Outcome.Success(review)
                }
                else Outcome.Error(Failure.ServerError(500, "Failed to create review"))
            } catch (e: Exception) {
                Outcome.Error(Failure.ServerError(500, e.message ?: "Database error"))
            }
        }
    }

    override suspend fun getProduct(id: String): Outcome<Product> = transaction {
        ProductsTable.selectAll().where { ProductsTable.id eq id }
            .map { Outcome.Success(it.toProduct()) }
            .singleOrNull() ?: Outcome.Error(Failure.ServerError(404, "Product not found"))
    }

    override suspend fun create(
        name: String,
        description: String,
        price: Double,
        category: String,
        stockQuantity: Int,
        imageUrl: String?,
        discountPercent: Int,
        gender: String?
    ): Outcome<Product> = transaction {
        val id = UUID.randomUUID().toString()
        ProductsTable.insert {
            it[ProductsTable.id] = id
            it[ProductsTable.name] = name
            it[ProductsTable.description] = description
            it[ProductsTable.category] = category
            it[ProductsTable.price] = price
            it[ProductsTable.stockQuantity] = stockQuantity
            it[ProductsTable.imageUrl] = imageUrl ?: ""
            it[ProductsTable.discountPercent] = discountPercent
            it[ProductsTable.gender] = gender
            it[ProductsTable.providerName] = "Mira Store"
            it[ProductsTable.createdAt] = System.currentTimeMillis()
        }
        val product = ProductsTable.selectAll().where { ProductsTable.id eq id }
            .map { it.toProduct() }
            .single()
            
        val event = DomainEvent.ProductChanged(
            eventId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            actorId = "admin",
            message = "Product $name created",
            productId = id
        )
        outboxRepository.save(null, DomainEventCodec.encode(event))
        
        Outcome.Success(product)
    }

    override suspend fun update(
        id: String,
        name: String?,
        description: String?,
        price: Double?,
        category: String?,
        stockQuantity: Int?,
        imageUrl: String?,
        discountPercent: Int?,
        gender: String?
    ): Outcome<Product> = transaction {
        ProductsTable.update({ ProductsTable.id eq id }) { stmt ->
            name?.let { stmt[ProductsTable.name] = it }
            description?.let { stmt[ProductsTable.description] = it }
            category?.let { stmt[ProductsTable.category] = it }
            price?.let { stmt[ProductsTable.price] = it }
            stockQuantity?.let { stmt[ProductsTable.stockQuantity] = it }
            imageUrl?.let { stmt[ProductsTable.imageUrl] = it }
            discountPercent?.let { stmt[ProductsTable.discountPercent] = it }
            gender?.let { stmt[ProductsTable.gender] = it }
        }
        
        val product = ProductsTable.selectAll().where { ProductsTable.id eq id }
            .map { it.toProduct() }
            .singleOrNull()
            
        if (product != null) {
            val event = DomainEvent.ProductChanged(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                actorId = "admin",
                message = "Product $id updated",
                productId = id
            )
            outboxRepository.save(null, DomainEventCodec.encode(event))
            Outcome.Success(product)
        } else Outcome.Error(Failure.ServerError(404, "Product not found"))
    }

    override suspend fun delete(id: String): Outcome<Unit> = transaction {
        val deletedRows = ProductsTable.deleteWhere { ProductsTable.id eq id }
        if (deletedRows > 0) Outcome.Success(Unit)
        else Outcome.Error(Failure.ServerError(404, "Product not found"))
    }

    fun findById(id: String): ProductDto? = transaction {
        ProductsTable.selectAll().where { ProductsTable.id eq id }
            .map { it.toProductDto() }
            .singleOrNull()
    }

    fun findAll(
        category: String? = null,
        subCategory: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
        query: String? = null,
        gender: String? = null
    ): ProductPageDto = transaction {
        val baseQuery = ProductsTable.selectAll()
        if (category != null) {
            baseQuery.andWhere { ProductsTable.category eq category }
        }
        if (subCategory != null) {
            baseQuery.andWhere { ProductsTable.subCategory eq subCategory }
        }
        if (gender != null) {
            baseQuery.andWhere { ProductsTable.gender eq gender }
        }

        query?.let { q ->
            val searchTerm = "%${q.lowercase()}%"
            baseQuery.andWhere {
                (ProductsTable.name.lowerCase() like searchTerm) or
                (ProductsTable.description.lowerCase() like searchTerm) or
                (ProductsTable.category.lowerCase() like searchTerm)
            }
        }

        val total = baseQuery.count()
        val products = baseQuery.limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map { it.toProductDto() }

        ProductPageDto(
            products = products,
            hasMore = ((page - 1) * pageSize) + products.size < total
        )
    }

    fun findAllCategories(): List<CategoryDto> = transaction {
        try {
            ProductCategoriesTable.selectAll().map { 
                val categoryName = it[ProductCategoriesTable.name]
                val count = ProductsTable.selectAll().where { ProductsTable.category eq categoryName }.count().toInt()
                
                CategoryDto(
                    id = it[ProductCategoriesTable.id],
                    name = categoryName,
                    imageUrl = it[ProductCategoriesTable.imageUrl] ?: "",
                    productCount = count
                )
            }
        } catch (e: Exception) {
            LoggerFactory.getLogger(ProductRepository::class.java).error("Failed to fetch product categories", e)
            throw e
        }
    }

    fun findLowStock(threshold: Int): List<ProductDto> = transaction {
        ProductsTable.selectAll().where { 
            (ProductsTable.stockQuantity lessEq threshold) and 
            (ProductsTable.name.lowerCase() notLike "%test%") 
        }
            .map { it.toProductDto() }
    }

    fun create(request: CreateProductRequest): ProductOperationResult = transaction {
        val id = UUID.randomUUID().toString()
        ProductsTable.insert {
            it[ProductsTable.id] = id
            it[ProductsTable.name] = request.name
            it[ProductsTable.description] = request.description
            it[ProductsTable.category] = request.category
            it[ProductsTable.subCategory] = request.subCategory
            it[ProductsTable.gender] = request.gender
            it[ProductsTable.price] = request.price
            it[ProductsTable.stockQuantity] = request.stockQuantity
            it[ProductsTable.imageUrl] = request.imageUrl ?: ""
            it[ProductsTable.discountPercent] = request.discountPercent
            it[ProductsTable.providerName] = "Mira Store"
            it[ProductsTable.createdAt] = System.currentTimeMillis()
        }
        findById(id)?.let { ProductOperationResult.Success(it) } ?: ProductOperationResult.FailureMsg("Failed to retrieve created product")
    }

    fun update(id: String, request: UpdateProductRequest): ProductOperationResult = transaction {
        val updatedRows = ProductsTable.update({ ProductsTable.id eq id }) {
            request.name?.let { name -> it[ProductsTable.name] = name }
            request.description?.let { description -> it[ProductsTable.description] = description }
            request.category?.let { category -> it[ProductsTable.category] = category }
            request.subCategory?.let { subCategory -> it[ProductsTable.subCategory] = subCategory }
            request.gender?.let { gender -> it[ProductsTable.gender] = gender }
            request.price?.let { price -> it[ProductsTable.price] = price }
            request.stockQuantity?.let { stockQuantity -> it[ProductsTable.stockQuantity] = stockQuantity }
            request.imageUrl?.let { imageUrl -> it[ProductsTable.imageUrl] = imageUrl }
            request.discountPercent?.let { discountPercent -> it[ProductsTable.discountPercent] = discountPercent }
        }
        if (updatedRows > 0) {
            findById(id)?.let { ProductOperationResult.Success(it) } ?: ProductOperationResult.NotFound
        } else ProductOperationResult.NotFound
    }

    fun deleteProductWithResult(id: String): ProductDeleteResult = transaction {
        val deletedRows = ProductsTable.deleteWhere { ProductsTable.id eq id }
        if (deletedRows > 0) ProductDeleteResult.Success
        else ProductDeleteResult.NotFound
    }


    fun getImagePath(id: String): String? = transaction {
        ProductsTable.select(ProductsTable.imageUrl)
            .where { ProductsTable.id eq id }
            .map { it[ProductsTable.imageUrl] }
            .singleOrNull()
    }

    fun getCategoryImagePath(categoryName: String): String? = transaction {
        ProductsTable.select(ProductsTable.imageUrl)
            .where { ProductsTable.category eq categoryName }
            .limit(1)
            .map { it[ProductsTable.imageUrl] }
            .firstOrNull()
    }

    fun findCategoryById(id: String): ProductCategoryOperationResult = transaction {
        ProductCategoriesTable.selectAll().where { ProductCategoriesTable.id eq id }
            .map {
                val categoryName = it[ProductCategoriesTable.name]
                val count = ProductsTable.selectAll().where { ProductsTable.category eq categoryName }.count().toInt()
                CategoryDto(
                    id = it[ProductCategoriesTable.id],
                    name = categoryName,
                    imageUrl = it[ProductCategoriesTable.imageUrl] ?: "",
                    productCount = count
                )
            }
            .singleOrNull()?.let { ProductCategoryOperationResult.Success(it) } ?: ProductCategoryOperationResult.NotFound
    }

    override suspend fun createCategory(name: String, imageUrl: String?, description: String?): Outcome<ProductCategory> = transaction {
        try {
            val id = UUID.randomUUID().toString()
            ProductCategoriesTable.insert {
                it[ProductCategoriesTable.id] = id
                it[ProductCategoriesTable.name] = name
                it[ProductCategoriesTable.imageUrl] = imageUrl
                it[ProductCategoriesTable.description] = description
            }
            val category = findCategoryById(id)
            if (category is ProductCategoryOperationResult.Success) {
                Outcome.Success(ProductCategory(
                    id = category.category.id,
                    name = category.category.name,
                    imageUrl = category.category.imageUrl,
                    productCount = category.category.productCount
                ))
            } else Outcome.Error(Failure.ServerError(500, "Failed to retrieve created category"))
        } catch (e: Exception) {
            Outcome.Error(Failure.ServerError(500, e.message ?: "Database error"))
        }
    }

    override suspend fun updateCategory(id: String, name: String?, imageUrl: String?, description: String?): Outcome<ProductCategory> = transaction {
        try {
            val oldCategory = ProductCategoriesTable.selectAll().where { ProductCategoriesTable.id eq id }
                .singleOrNull() ?: return@transaction Outcome.Error(Failure.ServerError(404, "Category not found"))
            
            val oldName = oldCategory[ProductCategoriesTable.name]
            
            val updatedRows = ProductCategoriesTable.update({ ProductCategoriesTable.id eq id }) {
                name?.let { n -> it[ProductCategoriesTable.name] = n }
                imageUrl?.let { img -> it[ProductCategoriesTable.imageUrl] = img }
                description?.let { d -> it[ProductCategoriesTable.description] = d }
            }
            
            if (updatedRows > 0 && name != null && name != oldName) {
                // Rename category in products table too
                ProductsTable.update({ ProductsTable.category eq oldName }) {
                    it[ProductsTable.category] = name
                }
            }
            
            if (updatedRows > 0) {
                val category = findCategoryById(id)
                if (category is ProductCategoryOperationResult.Success) {
                    Outcome.Success(ProductCategory(
                        id = category.category.id,
                        name = category.category.name,
                        imageUrl = category.category.imageUrl,
                        productCount = category.category.productCount
                    ))
                } else Outcome.Error(Failure.ServerError(500, "Failed to retrieve updated category"))
            }
            else Outcome.Error(Failure.ServerError(404, "Category not found"))
        } catch (e: Exception) {
            Outcome.Error(Failure.ServerError(500, e.message ?: "Database error"))
        }
    }

    override suspend fun deleteCategory(id: String): Outcome<Unit> = transaction {
        try {
            val category = ProductCategoriesTable.selectAll().where { ProductCategoriesTable.id eq id }
                .singleOrNull() ?: return@transaction Outcome.Error(Failure.ServerError(404, "Category not found"))
            
            val categoryName = category[ProductCategoriesTable.name]
            
            // Check if there are products in this category
            val hasProducts = ProductsTable.selectAll().where { ProductsTable.category eq categoryName }.any()
            if (hasProducts) {
                return@transaction Outcome.Error(Failure.ServerError(400, "Cannot delete category with associated products"))
            }
            
            val deletedRows = ProductCategoriesTable.deleteWhere { ProductCategoriesTable.id eq id }
            if (deletedRows > 0) Outcome.Success(Unit)
            else Outcome.Error(Failure.ServerError(404, "Category not found"))
        } catch (e: Exception) {
            Outcome.Error(Failure.ServerError(500, e.message ?: "Database error"))
        }
    }

    private fun ResultRow.toProduct() = transaction {
        val id = this@toProduct[ProductsTable.id]
        val category = this@toProduct[ProductsTable.category]
        val reviews = ReviewsTable.join(UsersTable, JoinType.INNER, ReviewsTable.userId, UsersTable.id)
            .selectAll().where { 
                (ReviewsTable.targetId eq id) and 
                (ReviewsTable.targetType eq "PRODUCT") and
                (ReviewsTable.isVisible eq true)
            }
            .map { it.toReview() }

        val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average() else 0.0
        
        // Dynamic discount calculation based on category promotions
        val autoDiscountPercent = getBestAutomaticDiscountPercent(category, this@toProduct[ProductsTable.price])
        val baseDiscountPercent = this@toProduct[ProductsTable.discountPercent]
        val effectiveDiscountPercent = maxOf(autoDiscountPercent, baseDiscountPercent)

        Product(
            id = id,
            name = this@toProduct[ProductsTable.name],
            category = category,
            description = this@toProduct[ProductsTable.description],
            imageUrl = this@toProduct[ProductsTable.imageUrl],
            price = this@toProduct[ProductsTable.price],
            stockQuantity = this@toProduct[ProductsTable.stockQuantity],
            discountPercent = effectiveDiscountPercent,
            averageRating = avgRating,
            reviewCount = reviews.size,
            gender = this@toProduct[ProductsTable.gender],
            providerName = this@toProduct[ProductsTable.providerName],
            reviews = reviews
        )
    }

    private fun getBestAutomaticDiscountPercent(categoryName: String, itemPrice: Double): Int {
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

    private fun ResultRow.toProductDto() = transaction {
        val id = this@toProductDto[ProductsTable.id]
        val category = this@toProductDto[ProductsTable.category]
        val reviews = ReviewsTable.join(UsersTable, JoinType.INNER, ReviewsTable.userId, UsersTable.id)
            .selectAll().where { 
                (ReviewsTable.targetId eq id) and 
                (ReviewsTable.targetType eq "PRODUCT") and
                (ReviewsTable.isVisible eq true)
            }
            .map {
                ReviewDto(
                    id = it[ReviewsTable.id],
                    userName = it[UsersTable.name],
                    userAvatarUrl = it[UsersTable.avatarUrl],
                    rating = it[ReviewsTable.rating],
                    comment = it[ReviewsTable.comment],
                    createdAtEpochSeconds = it[ReviewsTable.createdAt] / 1000,
                    targetId = it[ReviewsTable.targetId],
                    targetType = it[ReviewsTable.targetType]
                )
            }

        val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average() else 0.0

        // Dynamic discount calculation based on category promotions
        val autoDiscountPercent = getBestAutomaticDiscountPercent(category, this@toProductDto[ProductsTable.price])
        val baseDiscountPercent = this@toProductDto[ProductsTable.discountPercent]
        val effectiveDiscountPercent = maxOf(autoDiscountPercent, baseDiscountPercent)

        ProductDto(
            id = id,
            name = this@toProductDto[ProductsTable.name],
            category = category,
            subCategory = this@toProductDto[ProductsTable.subCategory],
            description = this@toProductDto[ProductsTable.description],
            imageUrl = this@toProductDto.getOrNull(ProductsTable.imageUrl),
            price = this@toProductDto[ProductsTable.price],
            discountPercent = effectiveDiscountPercent,
            stockQuantity = this@toProductDto[ProductsTable.stockQuantity],
            isAvailable = this@toProductDto[ProductsTable.isAvailable],
            averageRating = avgRating,
            reviewCount = reviews.size,
            gender = this@toProductDto[ProductsTable.gender],
            providerName = this@toProductDto[ProductsTable.providerName],
            reviews = reviews
        )
    }

    private fun ResultRow.toReview() = Review(
        id = this[ReviewsTable.id],
        userName = this[UsersTable.name],
        userAvatarUrl = this[UsersTable.avatarUrl],
        rating = this[ReviewsTable.rating],
        comment = this[ReviewsTable.comment],
        createdAtEpochSeconds = this[ReviewsTable.createdAt] / 1000
    )
}
