package iz.mkao.mirasalon.core.network.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String = "",
    val name: String,
    val imageUrl: String,
    val productCount: Int = 0,
)

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val category: String,
    val subCategory: String? = null,
    val description: String,
    val imageUrl: String? = null,
    val price: Double,
    val discountPercent: Int = 0,
    val stockQuantity: Int,
    val isAvailable: Boolean = true,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val gender: String? = null,
    val providerName: String = "Mira Store",
    val reviews: List<ReviewDto> = emptyList(),
)

@Serializable
data class ProductPageDto(
    val products: List<ProductDto>,
    val subCategories: List<String> = emptyList(),
    val hasMore: Boolean = false,
)

@Serializable
data class ReviewDto(
    val id: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val rating: Int = 0,
    val comment: String? = null,
    val createdAtEpochSeconds: Long = 0L,
    val targetId: String = "",
    val targetType: String = "",
    val targetName: String = "",
    val imageUrl: String? = null,
    val reviewId: String = ""
)

@Serializable
data class SubmitReviewRequest(
    val rating: Int,
    val comment: String,
    val targetId: String = "",
    val targetType: String = ""
)

@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String,
    val category: String,
    val subCategory: String? = null,
    val gender: String? = null,
    val price: Double,
    val stockQuantity: Int,
    val imageUrl: String? = null,
    val discountPercent: Int = 0
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val subCategory: String? = null,
    val gender: String? = null,
    val price: Double? = null,
    val stockQuantity: Int? = null,
    val imageUrl: String? = null,
    val discountPercent: Int? = null
)

@Serializable
data class CreateProductCategoryRequest(
    val name: String,
    val imageUrl: String? = null,
    val description: String? = null
)

@Serializable
data class UpdateProductCategoryRequest(
    val name: String? = null,
    val imageUrl: String? = null,
    val description: String? = null
)

@Serializable
data class ProductCountResponse(
    val count: Long
)
