package iz.mkao.mirasalon.server.data.tables

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.json

object UsersTable : Table("users") {
    val id = varchar("id", 50)
    val name = varchar("name", 100)
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20)
    val createdAt = long("created_at")
    val referralCode = varchar("referral_code", 50).nullable()
    val referredByUserId = varchar("referred_by_user_id", 50).references(id, onDelete = ReferenceOption.SET_NULL).nullable()
    val tokenVersion = integer("token_version").default(1)
    val avatarUrl = varchar("avatar_url", 255).nullable()
    val isActive = bool("is_active").default(true)
    val isDeleted = bool("is_deleted").default(false)
    val address = text("address").nullable()
    val firstName = varchar("first_name", 100).nullable()
    val lastName = varchar("last_name", 100).nullable()
    val phone = varchar("phone", 20).nullable()
    val gender = varchar("gender", 10).nullable()
    val dateOfBirth = varchar("date_of_birth", 20).nullable()
    val allergies = json<List<String>>("allergies", Json)

    override val primaryKey = PrimaryKey(id)
}

object RefreshTokensTable : Table("refresh_tokens") {
    val token = varchar("token", 255)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val expiresAt = long("expires_at")
    val revoked = bool("revoked").default(false)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(token)
}

object SalonsTable : Table("salons") {
    val id = varchar("id", 50)
    val name = varchar("name", 100)
    val address = text("address")
    val imageUrl = varchar("image_url", 255).nullable()
    val phone = varchar("phone", 20).nullable()
    val rating = double("rating").nullable()
    val openTime = varchar("open_time", 10).nullable()
    val closeTime = varchar("close_time", 10).nullable()
    val timezoneId = varchar("timezone_id", 50).nullable()
    val taxRatePercent = double("tax_rate_percent").default(0.0)

    override val primaryKey = PrimaryKey(id)
}

object SpecialistsTable : Table("specialists") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE).nullable()
    val salonId = varchar("salon_id", 50).references(SalonsTable.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val role = varchar("role", 50)
    val imageUrl = varchar("image_url", 255).nullable()
    val bio = text("bio").nullable()
    val status = varchar("status", 20).default("OFFLINE")
    val isActive = bool("is_active").default(true)
    val isDeleted = bool("is_deleted").default(false)
    val customersServed = integer("customers_served").default(0)
    val yearsOfExperience = integer("years_of_experience").default(0)
    val isVerified = bool("is_verified").default(false)
    val createdAt = long("created_at").default(System.currentTimeMillis())
    val gallery = json<List<String>>("gallery", Json).default(emptyList())

    override val primaryKey = PrimaryKey(id)
}

object ServiceCategoriesTable : Table("service_categories") {
    val id = varchar("id", 50)
    val name = varchar("name", 100)
    val iconName = varchar("icon_name", 50).nullable()
    val imageUrl = varchar("image_url", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

object ServicesTable : Table("services") {
    val id = varchar("id", 50)
    val name = varchar("name", 100)
    val description = text("description")
    val price = double("price")
    val durationMinutes = integer("duration_minutes")
    val categoryId = varchar("category_id", 50).references(ServiceCategoriesTable.id, onDelete = ReferenceOption.CASCADE)
    val imageUrl = varchar("image_url", 255).nullable()
    val subCategory = varchar("sub_category", 50).nullable()
    val rating = double("rating").default(0.0)
    val isActive = bool("is_active").default(true)
    val createdAt = long("created_at").default(System.currentTimeMillis())

    override val primaryKey = PrimaryKey(id)
}

object SpecialistServicesTable : Table("specialist_services") {
    val id = varchar("id", 50)
    val specialistId = varchar("specialist_id", 50).references(SpecialistsTable.id, onDelete = ReferenceOption.CASCADE)
    val serviceId = varchar("service_id", 50).references(ServicesTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}

object SpecialistShiftsTable : Table("specialist_shifts") {
    val id = varchar("id", 50)
    val specialistId = varchar("specialist_id", 50).references(SpecialistsTable.id, onDelete = ReferenceOption.CASCADE)
    val dayOfWeek = integer("day_of_week") // 1-7
    val startTime = varchar("start_time", 10)
    val endTime = varchar("end_time", 10)
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)
}

object SpecialistAbsencesTable : Table("specialist_absences") {
    val id = varchar("id", 50)
    val specialistId = varchar("specialist_id", 50).references(SpecialistsTable.id, onDelete = ReferenceOption.CASCADE)
    val startTime = long("start_time")
    val endTime = long("end_time")
    val reason = text("reason").nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object SpecialistClientNotesTable : Table("specialist_client_notes") {
    val id = varchar("id", 50)
    val specialistId = varchar("specialist_id", 50).references(SpecialistsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val note = text("note")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object AppointmentsTable : Table("appointments") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val salonId = varchar("salon_id", 50).references(SalonsTable.id, onDelete = ReferenceOption.CASCADE)
    val specialistId = varchar("specialist_id", 50).references(SpecialistsTable.id, onDelete = ReferenceOption.CASCADE)
    val status = varchar("status", 20)
    val dateTime = long("date_time")
    val servicesJson = text("services_json")
    val totalAmount = double("total_amount")
    val subtotalAmount = double("subtotal_amount").default(0.0)
    val taxAmount = double("tax_amount").default(0.0)
    val discountAmount = double("discount_amount").default(0.0)
    val durationMinutes = integer("duration_minutes")
    val createdAt = long("created_at")
    val promoCode = varchar("promo_code", 50).nullable()
    val reminderEnabled = bool("reminder_enabled").default(true)
    val reminderSent = bool("reminder_sent").default(false)
    val isReviewed = bool("is_reviewed").default(false)

    override val primaryKey = PrimaryKey(id)
}

object ProductCategoriesTable : Table("product_categories") {
    val id = varchar("id", 50)
    val name = varchar("name", 50).uniqueIndex()
    val imageUrl = varchar("image_url", 255).nullable()
    val description = text("description").nullable()

    override val primaryKey = PrimaryKey(id)
}

object ProductsTable : Table("products") {
    val id = varchar("id", 50)
    val name = varchar("name", 100)
    val category = varchar("category", 50) // Could reference ProductCategoriesTable.name if we wanted
    val subCategory = varchar("sub_category", 50).nullable()
    val description = text("description")
    val imageUrl = varchar("image_url", 255).default("")
    val price = double("price")
    val discountPercent = integer("discount_percent").default(0)
    val stockQuantity = integer("stock_quantity").default(0)
    val isAvailable = bool("is_available").default(true)
    val gender = varchar("gender", 10).nullable()
    val providerName = varchar("provider_name", 100).default("Mira Store")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object OrdersTable : Table("orders") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val salonId = varchar("salon_id", 50).references(SalonsTable.id, onDelete = ReferenceOption.CASCADE)
    val totalAmount = double("total_amount")
    val discountAmount = double("discount_amount").default(0.0)
    val status = varchar("status", 20)
    val createdAt = long("created_at")
    val shippingAddress = text("shipping_address").nullable()
    val paymentMethod = varchar("payment_method", 50).nullable()
    val trackingNumber = varchar("tracking_number", 100).nullable()
    val specialInstructions = text("special_instructions").nullable()
    val promoCode = varchar("promo_code", 50).nullable()
    val idempotencyKey = varchar("idempotency_key", 100).nullable()
    val expiresAt = long("expires_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object OrderItemsTable : Table("order_items") {
    val id = varchar("id", 50)
    val orderId = varchar("order_id", 50).references(OrdersTable.id, onDelete = ReferenceOption.CASCADE)
    val productId = varchar("product_id", 50).references(ProductsTable.id, onDelete = ReferenceOption.CASCADE)
    val quantity = integer("quantity")
    val price = double("price")
    val providerName = varchar("provider_name", 100).default("Mira Store")

    override val primaryKey = PrimaryKey(id)
}

object ReviewsTable : Table("reviews") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val targetId = varchar("target_id", 50)
    val targetType = varchar("target_type", 20) // SPECIALIST, SERVICE, PRODUCT, APPOINTMENT
    val rating = integer("rating")
    val comment = text("comment").nullable()
    val imageUrl = varchar("image_url", 255).nullable()
    val adminReply = text("admin_reply").nullable()
    val adminReplyAt = long("admin_reply_at").nullable()
    val isVisible = bool("is_visible").default(true)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object PromotionsTable : Table("promotions") {
    val id = varchar("id", 50)
    val code = varchar("code", 50).uniqueIndex()
    val title = varchar("title", 100)
    val ctaText = varchar("cta_text", 50).nullable()
    val description = text("description")
    val discountType = varchar("discount_type", 20) // PERCENTAGE, FIXED
    val discountValue = double("discount_value")
    val validFrom = long("valid_from").nullable()
    val validUntil = long("valid_until").nullable()
    val totalRedemptions = integer("total_redemptions").nullable()
    val perUserRedemptions = integer("per_user_redemptions").default(1)
    val currentUsageCount = integer("current_usage_count").default(0)
    val minOrderValue = double("min_order_value").nullable()
    val applicableServices = text("applicable_services").nullable() // Comma-separated IDs
    val applicableCategories = text("applicable_categories").nullable() // Comma-separated
    val targetUserId = varchar("target_user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE).nullable()
    val isFirstPurchaseOnly = bool("is_first_purchase_only").default(false)
    val stackable = bool("stackable").default(false)
    val status = varchar("status", 20).default("ACTIVE")
    val imageUrl = varchar("image_url", 255).nullable()
    val promoType = varchar("promo_type", 20).default("EXPERTS")
    val discountPercent = integer("discount_percent").default(0)
    val isActive = bool("is_active").default(true)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object PromotionUsagesTable : Table("promotion_usages") {
    val id = varchar("id", 50)
    val promotionId = varchar("promotion_id", 50).references(PromotionsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val orderId = varchar("order_id", 50).references(OrdersTable.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object OutboxTable : Table("outbox") {
    val eventId = varchar("event_id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val payload = text("payload")
    val createdAt = long("created_at")
    val dispatched = bool("dispatched").default(false)

    override val primaryKey = PrimaryKey(eventId)
}

object UserAddressesTable : Table("user_addresses") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val label = varchar("label", 50) // HOME, WORK, etc.
    val line1 = varchar("line1", 255)
    val line2 = varchar("line2", 255).nullable()
    val postalCode = varchar("postal_code", 20)
    val city = varchar("city", 100)
    val state = varchar("state", 100).nullable()
    val country = varchar("country", 100).default("")
    val isDefault = bool("is_default").default(false)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object UserNotificationPreferencesTable : Table("user_notification_preferences") {
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val pushEnabled = bool("push_enabled").default(true)
    val specialistMessagesEnabled = bool("specialist_messages_enabled").default(true)
    val bookingRemindersEnabled = bool("booking_reminders_enabled").default(true)
    val marketingEnabled = bool("marketing_enabled").default(false)
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(userId)
}
