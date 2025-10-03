package iz.mkao.mirasalon.presentation.products

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.ProductRepository
import iz.mkao.mirasalon.core.domain.repository.UploadRepository
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductsPresenter(
    private val repository: ProductRepository,
    private val uploadRepository: UploadRepository,
    private val realtimeGateway: RealtimeGateway,
    private val dispatcher: CoroutineDispatcher
) : Presenter<ProductsUiState> {

    @Composable
    override fun present(): ProductsUiState {
        var selectedCategory by remember {
            mutableStateOf(ProductsUiState().selectedCategory)
        }
        var searchQuery by remember {
            mutableStateOf("")
        }
        var currentPage by remember {
            mutableStateOf(ProductsUiState().currentPage)
        }
        var uploadProgress by remember {
            mutableStateOf(0f)
        }
        var refreshTrigger by remember {
            mutableStateOf(0L)
        }

        val scope = rememberCoroutineScope()

        val uiState by produceState(
            initialValue = ProductsUiState(
                selectedCategory = selectedCategory,
                searchQuery = searchQuery,
                currentPage = currentPage,
                isLoadingProducts = true,
                isLoadingCategories = true
            ),
            selectedCategory,
            searchQuery,
            currentPage,
            refreshTrigger
        ) {
            this.value = this.value.copy(
                isLoadingProducts = true,
                isLoadingCategories = true
            )

            try {
                coroutineScope {
                    val categoriesDeferred = async {
                        withContext(dispatcher) {
                            repository.getCategories()
                        }
                    }
                    val productsDeferred = async {
                        withContext(dispatcher) {
                            repository.getProducts(
                                category = selectedCategory,
                                subCategory = null,
                                page = currentPage,
                                pageSize = 20,
                                query = searchQuery.ifBlank { null }
                            )
                        }
                    }

                    val categoriesResult = categoriesDeferred.await()
                    val productsResult = productsDeferred.await()

                    var newCategories = value.categories
                    var newProducts = value.products
                    var totalPages = value.totalPages

                    if (categoriesResult is Outcome.Success) {
                        newCategories = categoriesResult.data.map { it.name }
                    }

                    if (productsResult is Outcome.Success) {
                        newProducts = productsResult.data.products
                        totalPages = if (productsResult.data.hasMore) {
                            currentPage + 1
                        } else {
                            currentPage
                        }
                    }

                    value = value.copy(
                        categories = newCategories,
                        products = newProducts,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        totalPages = totalPages,
                        isLoadingProducts = false,
                        isLoadingCategories = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                value = value.copy(
                    isLoadingProducts = false,
                    isLoadingCategories = false
                )
            }
        }

        LaunchedEffect(Unit) {
            realtimeGateway.events.collectLatest { event ->
                when (event) {
                    is DomainEvent.ProductChanged,
                    is DomainEvent.InventoryUpdated -> {
                        refreshTrigger = System.currentTimeMillis()
                    }
                    else -> Unit
                }
            }
        }

        return uiState.copy(
            uploadProgress = uploadProgress,
            eventSink = { event ->
                when (event) {
                    is ProductsEvent.Search -> {
                        searchQuery = event.query
                        currentPage = 1
                    }

                    is ProductsEvent.CategorySelected -> {
                        selectedCategory = event.category
                        currentPage = 1
                    }

                    is ProductsEvent.PageChanged -> {
                        currentPage = event.page
                    }

                    is ProductsEvent.UploadImage -> {
                        scope.launch {
                            uploadProgress = 0.1f
                            val result = withContext(dispatcher) {
                                val extension = event.fileName.substringAfterLast('.', "").lowercase()
                                val mimeType = when (extension) {
                                    "png" -> "image/png"
                                    "webp" -> "image/webp"
                                    else -> "image/jpeg"
                                }
                                uploadRepository.uploadImage(event.bytes, event.fileName, mimeType)
                            }
                            uploadProgress = 0.7f
                            val url = (result as? Outcome.Success)?.data
                            if (url != null) uploadProgress = 1.0f
                            event.onResult(url)
                        }
                    }

                    ProductsEvent.ResetUploadProgress -> {
                        uploadProgress = 0f
                    }

                    ProductsEvent.Refresh -> {
                        refreshTrigger = System.currentTimeMillis()
                    }

                    is ProductsEvent.CreateProduct -> {
                        scope.launch {
                            try {
                                when (repository.create(
                                    name = event.name,
                                    description = event.description,
                                    price = event.price,
                                    category = event.category,
                                    stockQuantity = event.stockQuantity,
                                    imageUrl = event.imageUrl,
                                    discountPercent = event.discountPercent
                                )) {
                                    is Outcome.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }
                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is ProductsEvent.UpdateProduct -> {
                        scope.launch {
                            try {
                                when (repository.update(
                                    id = event.id,
                                    name = event.name,
                                    description = event.description,
                                    price = event.price,
                                    category = event.category,
                                    stockQuantity = event.stockQuantity,
                                    imageUrl = event.imageUrl,
                                    discountPercent = event.discountPercent
                                )) {
                                    is Outcome.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }
                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is ProductsEvent.DeleteProduct -> {
                        scope.launch {
                            try {
                                when (repository.delete(event.id)) {
                                    is Outcome.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }
                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is ProductsEvent.CreateCategory -> {
                        scope.launch {
                            try {
                                when (repository.createCategory(event.name, event.imageUrl, event.description)) {
                                    is Outcome.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }
                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }
                }
            }
        )
    }
}
