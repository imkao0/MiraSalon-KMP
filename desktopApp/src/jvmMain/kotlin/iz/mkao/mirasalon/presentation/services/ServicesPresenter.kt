package iz.mkao.mirasalon.presentation.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.common.error.CommonError
import iz.mkao.mirasalon.core.common.result.NetworkResult
import iz.mkao.mirasalon.core.domain.outcome.toNetworkResult
import iz.mkao.mirasalon.core.domain.repository.AdminServiceRepository
import iz.mkao.mirasalon.core.domain.repository.UploadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ServicesPresenter(
    private val repository: AdminServiceRepository,
    private val uploadRepository: UploadRepository
) : Presenter<ServicesUiState> {

    @Composable
    override fun present(): ServicesUiState {
        var selectedCategoryId by remember {
            mutableStateOf<String?>(null)
        }
        var searchQuery by remember {
            mutableStateOf("")
        }
        var currentPage by remember {
            mutableStateOf(1)
        }
        var uploadProgress by remember {
            mutableStateOf(0f)
        }
        var refreshTrigger by remember {
            mutableStateOf(0L)
        }

        val scope = rememberCoroutineScope()

        val uiState by produceState(
            initialValue = ServicesUiState(
                selectedCategoryId = selectedCategoryId,
                searchQuery = searchQuery,
                currentPage = currentPage,
                isLoadingServices = true,
                isLoadingCategories = true
            ),
            selectedCategoryId,
            searchQuery,
            currentPage,
            refreshTrigger
        ) {
            this.value = this.value.copy(
                isLoadingServices = true,
                isLoadingCategories = true,
                error = null
            )

            try {
                coroutineScope {
                    val categoriesDeferred = async { repository.getCategories().toNetworkResult() }
                    val servicesDeferred = async {
                        repository.getServices(
                            categoryId = selectedCategoryId,
                            query = searchQuery.ifBlank { null }
                        ).toNetworkResult()
                    }

                    val categoriesResult = categoriesDeferred.await()
                    val servicesResult = servicesDeferred.await()

                    var newCategories = value.categories
                    var newServices = value.services
                    var error: CommonError? = null

                    if (categoriesResult is NetworkResult.Success) {
                        newCategories = categoriesResult.data
                    } else if (categoriesResult is NetworkResult.Error) {
                        error = CommonError.Network(categoriesResult.error.message ?: "Categories error")
                    }

                    if (servicesResult is NetworkResult.Success) {
                        newServices = servicesResult.data
                    } else if (servicesResult is NetworkResult.Error) {
                        error = error ?: CommonError.Network(servicesResult.error.message ?: "Services error")
                    }

                    value = value.copy(
                        categories = newCategories,
                        services = newServices,
                        selectedCategoryId = selectedCategoryId,
                        searchQuery = searchQuery,
                        error = error,
                        isLoadingServices = false,
                        isLoadingCategories = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                value = value.copy(
                    error = CommonError.Unknown(e.message ?: "System error"),
                    isLoadingServices = false,
                    isLoadingCategories = false
                )
            }
        }

        return uiState.copy(
            uploadProgress = uploadProgress,
            eventSink = { event ->
                when (event) {
                    is ServicesEvent.Search -> {
                        searchQuery = event.query
                        currentPage = 1
                    }

                    is ServicesEvent.CategorySelected -> {
                        selectedCategoryId = event.categoryId
                        currentPage = 1
                    }

                    is ServicesEvent.PageChanged -> {
                        currentPage = event.page
                    }

                    is ServicesEvent.CreateService -> {
                        scope.launch {
                            try {
                                when (repository.create(
                                    event.name,
                                    event.categoryId,
                                    event.subCategory,
                                    event.price,
                                    event.durationMinutes,
                                    event.description,
                                    event.imageUrl
                                ).toNetworkResult()) {
                                    is NetworkResult.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }

                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is ServicesEvent.UpdateService -> {
                        scope.launch {
                            try {
                                when (repository.update(
                                    event.id,
                                    event.name,
                                    event.categoryId,
                                    event.subCategory,
                                    event.price,
                                    event.durationMinutes,
                                    event.description,
                                    event.imageUrl
                                ).toNetworkResult()) {
                                    is NetworkResult.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }

                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is ServicesEvent.DeleteService -> {
                        scope.launch {
                            try {
                                when (repository.delete(event.id).toNetworkResult()) {
                                    is NetworkResult.Success -> {
                                        refreshTrigger = System.currentTimeMillis()
                                    }

                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                            }
                        }
                    }

                    is ServicesEvent.UploadImage -> {
                        scope.launch {
                            uploadProgress = 0.2f
                            val result = uploadRepository.uploadImage(event.bytes, event.fileName).toNetworkResult()
                            val url = (result as? NetworkResult.Success)?.data
                            uploadProgress = if (url != null) 1.0f else 0f
                            event.onResult(url)
                        }
                    }

                    ServicesEvent.ResetUploadProgress -> {
                        uploadProgress = 0f
                    }

                    ServicesEvent.Refresh -> {
                        refreshTrigger = System.currentTimeMillis()
                    }

                    is ServicesEvent.CreateCategory -> {
                        scope.launch {
                            try {
                                when (repository.createCategory(event.name, event.iconName, event.imageUrl).toNetworkResult()) {
                                    is NetworkResult.Success -> {
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
