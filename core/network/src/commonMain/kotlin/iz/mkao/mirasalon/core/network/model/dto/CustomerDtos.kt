package iz.mkao.mirasalon.core.network.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CustomerSummaryDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val totalAppointments: Int = 0,
    val totalSpend: Double = 0.0,
    val referralCode: String? = null,
    val avgRating: Double = 0.0,
    val createdAt: Long = 0L
)

@Serializable
data class CustomerDetailDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val dateOfBirth: String? = null,
    val joinedAt: Long,
    val referralCode: String? = null,
    val recentAppointments: List<SimpleAppointmentDto> = emptyList(),
    val recentOrders: List<SimpleOrderDto> = emptyList(),
    val reviews: List<CustomerReviewDto> = emptyList()
)

@Serializable
data class CustomerReviewDto(
    val id: String,
    val rating: Int,
    val comment: String? = null,
    val targetId: String? = null,
    val targetType: String? = null,
    val date: Long,
    val serviceName: String? = null,
    val specialistName: String? = null
)

@Serializable
data class SimpleOrderDto(
    val id: String,
    val status: String,
    val amount: Double,
    val date: Long
)

@Serializable
data class SimpleAppointmentDto(
    val id: String,
    val status: String,
    val dateTime: Long,
    val amount: Double
)

@Serializable
data class UpdateCustomerRequestDto(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val dateOfBirth: String? = null
)
