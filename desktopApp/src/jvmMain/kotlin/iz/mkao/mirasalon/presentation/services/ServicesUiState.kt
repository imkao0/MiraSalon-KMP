package iz.mkao.mirasalon.presentation.services

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.ServiceCategory
import iz.mkao.mirasalon.core.common.error.CommonError
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class ServicesUiState(
    val services: List<Service> = emptyList(),
    val categories: List<ServiceCategory> = emptyList(),
    val isLoadingServices: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val error: CommonError? = null,
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,

    val uploadProgress: Float = 0f,
    val successMessage: String? = null,
    val eventSink: (ServicesEvent) -> Unit = {}
) : CircuitUiState {
    val isLoading: Boolean get() = isLoadingServices || isLoadingCategories
}

/** Circuit UI events for the services admin screen. */
sealed interface ServicesEvent : CircuitUiEvent {
    data class Search(val query: String) : ServicesEvent
    data class CategorySelected(val categoryId: String?) : ServicesEvent
    data class PageChanged(val page: Int) : ServicesEvent
    data class CreateService(
        val name: String,
        val categoryId: String,
        val subCategory: String?,
        val price: Double,
        val durationMinutes: Int,
        val description: String,
        val imageUrl: String?
    ) : ServicesEvent
    data class UpdateService(
        val id: String,
        val name: String?,
        val categoryId: String?,
        val subCategory: String?,
        val price: Double?,
        val durationMinutes: Int?,
        val description: String?,
        val imageUrl: String?
    ) : ServicesEvent
    data class DeleteService(val id: String) : ServicesEvent
    data class UploadImage(val bytes: ByteArray, val fileName: String, val onResult: (String?) -> Unit) : ServicesEvent
    data object ResetUploadProgress : ServicesEvent
    data object Refresh : ServicesEvent
    data class CreateCategory(val name: String, val iconName: String?, val imageUrl: String?) : ServicesEvent
}
