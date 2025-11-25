package iz.mkao.mirasalon.feature.products.data.mapper

import iz.mkao.mirasalon.core.domain.model.Product
import iz.mkao.mirasalon.core.domain.model.ProductCategory
import iz.mkao.mirasalon.core.domain.model.ProductPage
import iz.mkao.mirasalon.core.domain.model.Review
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.CategoryDto
import iz.mkao.mirasalon.core.network.model.dto.ProductDto
import iz.mkao.mirasalon.core.network.model.dto.ProductPageDto
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto

fun ProductDto.toDomain(): Product = Product(
    id = id,
    name = name,
    category = category,
    description = description,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl) ?: "",
    price = price,
    discountPercent = discountPercent,
    stockQuantity = stockQuantity,
    averageRating = averageRating,
    reviewCount = reviewCount,
    providerName = providerName,
    reviews = reviews.map { it.toDomain() },
)

fun CategoryDto.toDomain(): ProductCategory = ProductCategory(
    id = id,
    name = name,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl) ?: "",
    productCount = productCount,
)

fun ReviewDto.toDomain(): Review = Review(
    id = id,
    userName = userName,
    userAvatarUrl = ApiEndpoints.resolveImageUrl(userAvatarUrl),
    rating = rating,
    comment = comment,
    createdAtEpochSeconds = createdAtEpochSeconds,
)

fun ProductPageDto.toDomain(): ProductPage = ProductPage(
    products = products.map { it.toDomain() },
    subCategories = subCategories,
    hasMore = hasMore,
)
