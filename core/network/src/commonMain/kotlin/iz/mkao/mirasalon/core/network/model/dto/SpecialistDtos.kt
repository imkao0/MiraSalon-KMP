package iz.mkao.mirasalon.core.network.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpecialistDto(
    val id: String,
    val userId: String? = null,
    val name: String,
    val role: String,
    val imageUrl: String? = null,
    val bio: String? = null,
    val rating: Double = 0.0,
    val salonId: String,
    val services: List<ServiceDto> = emptyList(),
    val isAvailable: Boolean = true,
    val isOnline: Boolean = false,
    val customersCount: Int = 0,
    val yearsOfExperience: Int = 0,
    val reviews: List<SpecialistReviewDto> = emptyList(),
    val status: String = "ACTIVE",
    val isActive: Boolean = true,
    val customersServed: Int = 0,
    val isVerified: Boolean = false
)

@Serializable
data class SpecialistListResponseDto(
    val items: List<SpecialistDto>
)

@Serializable
data class SpecialistReviewDto(
    val id: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val rating: Int,
    val comment: String,
    val createdAtEpochSeconds: Long
)

@Serializable
data class SpecialistShiftDto(
    val id: String? = null,
    val specialistId: String? = null,
    val dayOfWeek: Int, // 1 (Monday) to 7 (Sunday)
    val startTime: String,
    val endTime: String,
    val isWorkingDay: Boolean = true
)

@Serializable
data class CreateSpecialistRequestDto(
    val userId: String? = null,
    val name: String,
    val role: String,
    val salonId: String,
    val imageUrl: String? = null,
    val bio: String? = null,
    val customersServed: Int = 0,
    val yearsOfExperience: Int = 0,
    val serviceIds: List<String> = emptyList()
)

@Serializable
data class UpdateSpecialistRequestDto(
    val name: String? = null,
    val role: String? = null,
    val imageUrl: String? = null,
    val bio: String? = null,
    val isAvailable: Boolean? = null,
    val status: String? = null,
    val customersServed: Int? = null,
    val yearsOfExperience: Int? = null,
    val serviceIds: List<String>? = null
)

@Serializable
data class SpecialistAvailabilityDto(
    val id: String = "",
    val name: String = "",
    val role: String = "",
    val imageUrl: String? = null,
    val isBusy: Boolean = false,
    val busyUntil: Long? = null,
    val nextAvailableTime: String? = null,
    val specialistId: String = "",
    val date: String = "",
    val availableSlots: List<TimeSlotDto> = emptyList()
)

@Serializable
data class TimeSlotDto(
    val startTime: Long,
    val endTime: Long,
    val isAvailable: Boolean,
    val formattedTime: String = ""
)

@Serializable
data class UpdateSpecialistStatusRequest(
    val status: String
)

@Serializable
data class UpdateSpecialistActiveRequest(
    val isActive: Boolean
)
