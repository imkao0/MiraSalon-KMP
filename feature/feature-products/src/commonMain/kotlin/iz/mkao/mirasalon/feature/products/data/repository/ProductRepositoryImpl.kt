package iz.mkao.mirasalon.feature.products.data.repository

import iz.mkao.mirasalon.core.database.datasource.ProductLocalDataSource
import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.ProductPage
import iz.mkao.mirasalon.core.domain.model.Review
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.ProductRepository
import iz.mkao.mirasalon.core.network.model.dto.CreateProductCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.CreateProductRequest
import iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateProductCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateProductRequest
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.feature.products.data.mapper.toDomain
import iz.mkao.mirasalon.feature.products.data.network.api.ProductsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class ProductRepositoryImpl(
    private val api: ProductsApi,
    private val localDataSource: ProductLocalDataSource,
    private val realtimeGateway: RealtimeGateway,
    private val repositoryScope: CoroutineScope
) : ProductRepository {

    init {
        observeRealtimeEvents()
        repositoryScope.launch { 
            // Initial refresh
            getProducts(null, null, 1, 20, null, null)
        }
    }

    private fun observeRealtimeEvents() {
        repositoryScope.launch {
            realtimeGateway.events.collectLatest { event ->
                when (event) {
                    is DomainEvent.InventoryUpdated -> {
                        localDataSource.updateProductStock(event.productId, event.newStock)
                    }
                    is DomainEvent.ProductChanged -> {
                        getProduct(event.productId)
                    }
                    is DomainEvent.ReviewSubmitted -> {
                        if (event.targetType == "PRODUCT") {
                            getProduct(event.targetId)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    override suspend fun getCategories(): Outcome<List<ProductCategory>> =
        api.fetchCategories().let { result ->
            when (result) {
                is Outcome.Success -> Outcome.Success(result.data.map { it.toDomain() })
                is Outcome.Error -> Outcome.Error(result.failure)
                is Outcome.Loading -> Outcome.Loading
            }
        }

    override suspend fun getProducts(
        category: String?,
        subCategory: String?,
        page: Int,
        pageSize: Int,
        query: String?,
        gender: String?
    ): Outcome<ProductPage> {
        val networkResult = api.fetchProducts(category, subCategory, page, pageSize, query, gender).let { result ->
            when (result) {
                is Outcome.Success -> {
                    val domainPage = result.data.toDomain()
                    localDataSource.saveProducts(domainPage.products)
                    Outcome.Success(domainPage)
                }
                is Outcome.Error -> Outcome.Error(result.failure)
                is Outcome.Loading -> Outcome.Loading
            }
        }
        return networkResult
    }

    override suspend fun getProduct(id: String): Outcome<Product> =
        api.fetchProduct(id).let { result ->
            when (result) {
                is Outcome.Success -> {
                    val domain = result.data.toDomain()
                    localDataSource.saveProducts(listOf(domain))
                    Outcome.Success(domain)
                }
                is Outcome.Error -> Outcome.Error(result.failure)
                is Outcome.Loading -> Outcome.Loading
            }
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
    ): Outcome<Product> =
        api.createProduct(
            CreateProductRequest(
                name = name,
                description = description,
                price = price,
                category = category,
                stockQuantity = stockQuantity,
                imageUrl = imageUrl,
                discountPercent = discountPercent,
                gender = gender
            )
        ).let { result ->
            when (result) {
                is Outcome.Success -> {
                    val domain = result.data.toDomain()
                    localDataSource.saveProducts(listOf(domain))
                    Outcome.Success(domain)
                }
                is Outcome.Error -> Outcome.Error(result.failure)
                is Outcome.Loading -> Outcome.Loading
            }
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
    ): Outcome<Product> =
        api.updateProduct(
            id,
            UpdateProductRequest(
                name = name,
                description = description,
                price = price,
                category = category,
                stockQuantity = stockQuantity,
                imageUrl = imageUrl,
                discountPercent = discountPercent,
                gender = gender
            )
        ).let { result ->
            when (result) {
                is Outcome.Success -> {
                    val domain = result.data.toDomain()
                    localDataSource.saveProducts(listOf(domain))
                    Outcome.Success(domain)
                }
                is Outcome.Error -> Outcome.Error(result.failure)
                is Outcome.Loading -> Outcome.Loading
            }
        }

    override suspend fun delete(id: String): Outcome<Unit> =
        api.deleteProduct(id).let { result ->
            when (result) {
                is Outcome.Success -> {
                    // We might want a localDataSource.deleteProduct(id) here
                    Outcome.Success(Unit)
                }
                is Outcome.Error -> Outcome.Error(result.failure)
                is Outcome.Loading -> Outcome.Loading
            }
        }

    override suspend fun createCategory(name: String, imageUrl: String?, description: String?): Outcome<ProductCategory> =
        api.createCategory(CreateProductCategoryRequest(name, imageUrl, description)).let { result ->
            when (result) {
                is Outcome.Success -> Outcome.Success(result.data.toDomain())
                is Outcome.Error -> Outcome.Error(result.failure)
                is Outcome.Loading -> Outcome.Loading
            }
        }

    override suspend fun updateCategory(id: String, name: String?, imageUrl: String?, description: String?): Outcome<ProductCategory> =
        api.updateCategory(id, UpdateProductCategoryRequest(name, imageUrl, description)).let { result ->
            when (result) {
                is Outcome.Success -> Outcome.Success(result.data.toDomain())
                is Outcome.Error -> Outcome.Error(result.failure)
                is Outcome.Loading -> Outcome.Loading
            }
        }

    override suspend fun deleteCategory(id: String): Outcome<Unit> =
        api.deleteCategory(id).let { result ->
            when (result) {
                is Outcome.Success<*> -> Outcome.Success(Unit)
                is Outcome.Error -> Outcome.Error(result.failure)
                is Outcome.Loading -> Outcome.Loading
            }
        }

    override fun observeProducts(
        category: String?,
        subCategory: String?,
        page: Int,
        pageSize: Int,
        query: String?,
        gender: String?
    ): Flow<Outcome<ProductPage>> {
        val localFlow = if (category != null) {
            localDataSource.observeProductsByCategory(category)
        } else {
            localDataSource.observeAllProducts()
        }

        return localFlow.map { localProducts ->
            val filtered = if (query != null) {
                localProducts.filter { it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
            } else localProducts
            
            Outcome.Success(ProductPage(products = filtered, subCategories = emptyList(), hasMore = false))
        }.onStart {
            repositoryScope.launch {
                getProducts(category, subCategory, page, pageSize, query, gender)
            }
        }
    }

    override suspend fun getReviews(productId: String): Outcome<List<Review>> =
        api.fetchReviews(productId).let { result ->
            when (result) {
                is Outcome.Success -> Outcome.Success(result.data.map { it.toDomain() })
                is Outcome.Error -> Outcome.Error(result.failure)
                is Outcome.Loading -> Outcome.Loading
            }
        }

    override suspend fun submitReview(
        productId: String,
        rating: Int,
        comment: String,
        userId: String?
    ): Outcome<Review> =
        api.submitReview(productId, SubmitReviewRequest(rating, comment, targetId = productId, targetType = "PRODUCT")).let { result ->
            when (result) {
                is Outcome.Success -> Outcome.Success(result.data.toDomain())
                is Outcome.Error -> Outcome.Error(result.failure)
                is Outcome.Loading -> Outcome.Loading
            }
        }
}
