package iz.mkao.mirasalon.core.network.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SalonDto(
    val id: String,
    val name: String,
    val address: String,
    val imageUrl: String? = null,
    val phone: String? = null,
    val rating: Double = 0.0,
    val openTime: String? = "08:00",
    val closeTime: String? = "20:00",
    val timezoneId: String? = "UTC"
)

@Serializable
data class SalonPaginatedResponseDto(
    val items: List<SalonDto>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class UpdateSalonRequest(
    val name: String? = null,
    val address: String? = null,
    val imageUrl: String? = null,
    val phone: String? = null,
    val openTime: String? = null,
    val closeTime: String? = null,
    val timezoneId: String? = null
)

@Serializable
data class ServiceDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    val durationMinutes: Int,
    val imageUrl: String? = null,
    val categoryId: String? = null,
    val salonId: String? = null,
    val subCategory: String? = null,
    val discountPercent: Int = 0,
    val rating: Double = 0.0,
    val reviews: List<ReviewDto> = emptyList(),
    val isActive: Boolean = true
)

@Serializable
data class ServiceListResponseDto(
    val items: List<ServiceDto>
)

@Serializable
data class ServiceCategoryDto(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val salonId: String? = null
)

@Serializable
data class CreateServiceCategoryRequest(
    val name: String,
    val iconName: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class UpdateServiceCategoryRequest(
    val name: String? = null,
    val iconName: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class SalonHomeDto(
    val categories: List<ServiceCategoryDto> = emptyList(),
    val specialists: List<SpecialistDto> = emptyList(),
    val promotions: List<PromotionDto> = emptyList(),
    val isLoggedIn: Boolean = false,
)

@Serializable
data class CreateServiceRequestDto(
    val name: String,
    val description: String? = null,
    val price: Double,
    val durationMinutes: Int,
    val categoryId: String,
    val imageUrl: String? = null,
    val subCategory: String? = null,
    val rating: Double? = null
)

@Serializable
data class UpdateServiceRequestDto(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val durationMinutes: Int? = null,
    val categoryId: String? = null,
    val imageUrl: String? = null,
    val subCategory: String? = null,
    val rating: Double? = null,
    val isActive: Boolean? = null
)

@Serializable
data class DashboardDto(
    val totalSales: Double = 0.0,
    val totalAppointments: Int = 0,
    val totalRevenue: Double = 0.0,
    val confirmedAppointments: Int = 0,
    val cancelledAppointments: Int = 0
)

@Serializable
data class PaymentStatsDto(
    val succeeded: Int = 0,
    val refunded: Int = 0,
    val failed: Int = 0
)

@Serializable
data class ActivityEventDto(
    val id: String = "",
    val type: String = "",
    val customerEmail: String = "",
    val status: String = "",
    val timestamp: String = "",
    val imageUrl: String? = null,
    val serviceName: String = "",
    val details: String = ""
)

@Serializable
data class AppointmentStatsDto(
    val days: Int = 0,
    val points: List<AppointmentDailyPoint> = emptyList(),
    val totalConfirmed: Int = 0,
    val totalCancelled: Int = 0,
    val revenue: Double = 0.0,
    val appointmentRevenue: Double = 0.0,
    val productRevenue: Double = 0.0,
    val revenueGrowth: Double = 0.0
)

@Serializable
data class AppointmentDailyPoint(
    val date: String = "",
    val confirmed: Int = 0,
    val cancelled: Int = 0,
    val newClients: Int = 0,
    val returningClients: Int = 0
)

@Serializable
data class SpecialistPerformanceDto(
    val specialistId: String = "",
    val name: String = "",
    val appointmentCount: Int = 0,
    val completionRate: Double = 0.0,
    val revenue: Double = 0.0,
    val revenueGrowth: Double = 0.0,
    val targetAchievement: Double = 0.0
)

@Serializable
data class ServicePopularityDto(
    val serviceId: String = "",
    val name: String = "",
    val count: Int = 0,
    val ratio: Double = 0.0
)

@Serializable
data class SalesTrendDto(
    val days: Int = 0,
    val points: List<SalesTrendPoint> = emptyList(),
    val appointmentRevenue: Double = 0.0,
    val productRevenue: Double = 0.0,
    val revenueGrowth: Double = 0.0
)

@Serializable
data class SalesTrendPoint(
    val date: String = "",
    val sales: Double = 0.0,
    val appointments: Int = 0
)

@Serializable
data class StreamTokenRequest(
    val userId: String
)

@Serializable
data class StreamTokenResponse(
    val token: String,
    val userId: String = "",
    val apiKey: String = "",
    val appId: String = "",
    val expiresAt: Long = 0L
)

@Serializable
data class UploadResponse(
    val url: String,
    val fileName: String = "",
    val sizeBytes: Long = 0L,
    val contentType: String = ""
)
