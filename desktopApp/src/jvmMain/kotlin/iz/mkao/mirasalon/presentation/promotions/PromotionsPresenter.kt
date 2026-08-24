package iz.mkao.mirasalon.presentation.promotions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.common.result.NetworkResult
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.domain.outcome.toNetworkResult
import iz.mkao.mirasalon.core.domain.repository.AdminPromotionRepository
import iz.mkao.mirasalon.core.domain.repository.AdminServiceRepository
import iz.mkao.mirasalon.core.domain.repository.ProductRepository
import iz.mkao.mirasalon.core.domain.repository.UploadRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PromotionsPresenter(
    private val repository: AdminPromotionRepository,
    private val uploadRepository: UploadRepository,
    private val adminServiceRepository: AdminServiceRepository,
    private val productRepository: ProductRepository
) : Presenter<PromotionsUiState> {

    @Composable
    override fun present(): PromotionsUiState {
        var promotions by remember { mutableStateOf(PromotionsUiState().promotions) }
        var services by remember { mutableStateOf(emptyList<Service>()) }
        var productCategories by remember { mutableStateOf(emptyList<ProductCategory>()) }
        var serviceCategories by remember { mutableStateOf(emptyList<ServiceCategory>()) }
        var searchQuery by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var uploadProgress by remember { mutableStateOf(0f) }
        val scope = rememberCoroutineScope()
        var loadJob by remember { mutableStateOf<Job?>(null) }

        fun loadPromotions(query: String) {
            loadJob?.cancel()
            loadJob = scope.launch {
                isLoading = true
                val result = repository.getAll(query = query.ifBlank { null }).toNetworkResult()
                if (result is NetworkResult.Success) promotions = result.data
                isLoading = false
            }
        }

        fun loadMetadata() {
            scope.launch {
                adminServiceRepository.getCategories().toNetworkResult().let {
                    if (it is NetworkResult.Success) serviceCategories = it.data
                }
                adminServiceRepository.getServices(null, null).toNetworkResult().let {
                    if (it is NetworkResult.Success) services = it.data
                }
                productRepository.getCategories().toNetworkResult().let {
                    if (it is NetworkResult.Success) productCategories = it.data
                }
            }
        }

        LaunchedEffect(Unit) {
            loadPromotions(searchQuery)
            loadMetadata()
        }

        return PromotionsUiState(
            promotions = promotions,
            services = services,
            productCategories = productCategories,
            serviceCategories = serviceCategories,
            searchQuery = searchQuery,
            isLoading = isLoading,
            uploadProgress = uploadProgress
        ) { event ->
            when (event) {
                is PromotionsEvent.Search -> {
                    searchQuery = event.query
                    loadPromotions(event.query)
                }
                is PromotionsEvent.CreatePromotion -> scope.launch {
                    isLoading = true
                    when (repository.create(event.promotion).toNetworkResult()) {
                        is NetworkResult.Success -> loadPromotions(searchQuery)
                        else -> isLoading = false
                    }
                }
                is PromotionsEvent.UpdatePromotion -> scope.launch {
                    isLoading = true
                    when (repository.update(event.promotion).toNetworkResult()) {
                        is NetworkResult.Success -> loadPromotions(searchQuery)
                        else -> isLoading = false
                    }
                }
                is PromotionsEvent.DeletePromotion -> scope.launch {
                    isLoading = true
                    when (repository.delete(event.id).toNetworkResult()) {
                        is NetworkResult.Success -> loadPromotions(searchQuery)
                        else -> isLoading = false
                    }
                }
                is PromotionsEvent.UploadImage -> scope.launch {
                    uploadProgress = 0.1f
                    val result = uploadRepository.uploadImage(event.bytes, event.name, "image/jpeg").toNetworkResult()
                    val url = (result as? NetworkResult.Success)?.data
                    uploadProgress = if (url != null) 1.0f else 0f
                    event.onResult(url)
                }
                PromotionsEvent.ResetUploadProgress -> {
                    uploadProgress = 0f
                }
                PromotionsEvent.ClearAll -> scope.launch {
                    isLoading = true
                    when (repository.clearAll().toNetworkResult()) {
                        is NetworkResult.Success -> loadPromotions(searchQuery)
                        else -> isLoading = false
                    }
                }
                PromotionsEvent.Refresh -> loadPromotions(searchQuery)
            }
        }
    }
}
