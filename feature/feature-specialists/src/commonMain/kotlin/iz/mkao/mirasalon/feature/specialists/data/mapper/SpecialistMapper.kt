package iz.mkao.mirasalon.feature.specialists.data.mapper

import iz.mkao.mirasalon.core.database.dao.SpecialistWithServices
import iz.mkao.mirasalon.core.database.entity.ServiceEntity
import iz.mkao.mirasalon.core.database.entity.SpecialistEntity
import iz.mkao.mirasalon.core.database.entity.SpecialistServiceEntity
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.model.SpecialistReview
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.core.network.model.dto.ServiceDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistReviewDto

fun SpecialistDto.toDomain(): Specialist = Specialist(
    id = id,
    name = name,
    role = role,
    salonId = salonId,
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    rating = rating,
    isOnline = isOnline,
    customersCount = customersServed,
    yearsOfExperience = yearsOfExperience,
    bio = bio ?: "",
    services = services.map { it.toDomainService() },
    reviews = reviews.map { it.toDomain() }
)

fun ServiceDto.toDomainService(): Service = Service(
    id = id,
    name = name,
    description = description ?: "",
    durationMinutes = durationMinutes,
    price = price,
    discountPercent = discountPercent,
    categoryId = categoryId ?: "",
    imageUrl = ApiEndpoints.resolveImageUrl(imageUrl) ?: ApiEndpoints.getServicePlaceholder(name)
)

fun ServiceEntity.toDomainService(): Service = Service(
    id = id,
    name = name,
    description = description,
    durationMinutes = durationMinutes,
    price = price,
    discountPercent = discountPercent,
    categoryId = categoryId,
    imageUrl = imageUrl ?: ApiEndpoints.getServicePlaceholder(name)
)

fun SpecialistReviewDto.toDomain(): SpecialistReview = SpecialistReview(
    id = id,
    userName = userName,
    userAvatarUrl = ApiEndpoints.resolveImageUrl(userAvatarUrl),
    rating = rating,
    comment = comment,
    createdAtEpochSeconds = createdAtEpochSeconds,
)

fun SpecialistDto.toEntity() = SpecialistEntity(
    id = id,
    name = name,
    title = role,
    avatarUrl = ApiEndpoints.resolveImageUrl(imageUrl),
    bio = bio,
    rating = rating,
    reviewCount = reviews.size,
    salonId = salonId,
    customersCount = customersServed,
    yearsOfExperience = yearsOfExperience,
    isOnline = isOnline
)

fun SpecialistDto.toServiceEntities(): List<ServiceEntity> = services.map { dto ->
    ServiceEntity(
        id = dto.id,
        name = dto.name,
        description = dto.description ?: "",
        durationMinutes = dto.durationMinutes,
        price = dto.price,
        categoryId = dto.categoryId ?: "",
        discountPercent = dto.discountPercent,
        imageUrl = dto.imageUrl
    )
}

fun SpecialistDto.toSpecialistServiceRelations(): List<SpecialistServiceEntity> = services.map { service ->
    SpecialistServiceEntity(specialistId = id, serviceId = service.id)
}

fun SpecialistEntity.toDomain() = Specialist(
    id = id,
    name = name,
    role = title ?: "",
    salonId = salonId ?: "",
    imageUrl = avatarUrl,
    rating = rating,
    isOnline = isOnline,
    customersCount = customersCount,
    yearsOfExperience = yearsOfExperience,
    bio = bio ?: "",
    services = emptyList(),
    reviews = emptyList()
)

fun SpecialistWithServices.toDomain(): Specialist = specialist.toDomain().copy(
    services = services.map { it.toDomainService() }
)
