package iz.mkao.mirasalon.core.domain.model

/** A single point on the sales trend chart. */
data class SalesDataPoint(
    val date: String,
    val amount: Double,
    val appointments: Int = 0
)

/** Revenue-over-time series consumed by the analytics screen. */
data class SalesTrend(
    val points: List<SalesDataPoint> = emptyList(),
    val totalRevenue: Double = 0.0,
    val appointmentRevenue: Double = 0.0,
    val productRevenue: Double = 0.0,
    val revenueGrowth: Double = 0.0,
) {
    val maxAmount: Double
        get() = points.maxOfOrNull { it.amount } ?: 0.0
}

/** Popularity of one service, used for the "top services" chart. */
data class ServicePopularity(
    val serviceId: String,
    val serviceName: String,
    val name: String = "",
    val count: Int = 0,
    val bookingCount: Int = 0,
    val revenue: Double = 0.0,
    val ratio: Float = 0f
)

/** Revenue/booking performance of one specialist. */
data class SpecialistPerformance(
    val specialistId: String,
    val specialistName: String,
    val name: String = "",
    val bookingCount: Int = 0,
    val revenue: Double = 0.0,
    val averageRating: Double = 0.0,
    val targetAchievement: Float = 0f
)

/** A product at or below its low-stock threshold. */
data class LowStockProduct(
    val productId: String,
    val productName: String,
    val stockQuantity: Int,
    val lowStockThreshold: Int,
)

/** A single entry in the dashboard activity feed. */
data class ActivityEvent(
    val id: String,
    val type: String,
    val message: String,
    val timestamp: Long,
    val actorId: String? = null,
    val imageUrl: String? = null,
    val serviceName: String? = null,
    val customerEmail: String = "",
    val status: String = ""
)
