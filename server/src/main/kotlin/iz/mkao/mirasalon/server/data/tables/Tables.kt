package iz.mkao.mirasalon.server.data.tables

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.json

object UsersTable : Table("users") {
    val id = varchar("id", 50)
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 50)
    val createdAt = long("created_at")
    val referralCode = varchar("referral_code", 50).nullable().uniqueIndex("users_referral_code_key")
    val referredByUserId = varchar("referred_by_user_id", 50).references(id, onDelete = ReferenceOption.SET_NULL).nullable()
    val tokenVersion = integer("token_version").default(1)
    val avatarUrl = varchar("avatar_url", 512).nullable()
    val isActive = bool("is_active").default(true)
    val isDeleted = bool("is_deleted").default(false).index("users_is_deleted_idx")
    val address = text("address").nullable()
    val firstName = varchar("first_name", 100).nullable()
    val lastName = varchar("last_name", 100).nullable()
    val phone = varchar("phone", 20).nullable()
    val gender = varchar("gender", 20).nullable()
    val dateOfBirth = varchar("date_of_birth", 20).nullable()
    val allergies = json<List<String>>("allergies", Json).nullable()

    override val primaryKey = PrimaryKey(id)
}

object RefreshTokensTable : Table("refresh_tokens") {
    val token = varchar("token", 255)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index("idx_refresh_tokens_user_id")
    val expiresAt = long("expires_at").index("idx_refresh_tokens_expires_at")
    val revoked = bool("revoked").default(false)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(token)
}

object SalonsTable : Table("salons") {
    val id = varchar("id", 50)
    val name = varchar("name", 255)
    val address = text("address")
    val imageUrl = varchar("image_url", 512).nullable()
    val phone = varchar("phone", 50).nullable()
    val rating = double("rating").nullable()
    val openTime = varchar("open_time", 10).nullable()
    val closeTime = varchar("close_time", 10).nullable()
    val timezoneId = varchar("timezone_id", 100).nullable()
    val taxRatePercent = double("tax_rate_percent").default(0.0)

    override val primaryKey = PrimaryKey(id)
}

object SpecialistsTable : Table("specialists") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).nullable().uniqueIndex("idx_specialists_user_id")
    val salonId = varchar("salon_id", 50).references(SalonsTable.id, onDelete = ReferenceOption.CASCADE).index("idx_specialists_salon_id")
    val name = varchar("name", 255)
    val role = varchar("role", 100)
    val imageUrl = varchar("image_url", 512).nullable()
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
    val name = varchar("name", 255)
    val iconName = varchar("icon_name", 255).nullable()
    val imageUrl = varchar("image_url", 512).nullable()

    override val primaryKey = PrimaryKey(id)
}

object ServicesTable : Table("services") {
    val id = varchar("id", 50)
    val name = varchar("name", 255)
    val description = text("description")
    val price = double("price")
    val durationMinutes = integer("duration_minutes")
    val categoryId = varchar("category_id", 50).references(ServiceCategoriesTable.id, onDelete = ReferenceOption.CASCADE).index("idx_services_category_id")
    val imageUrl = varchar("image_url", 512).nullable()
    val subCategory = varchar("sub_category", 100).nullable()
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

    init {
        index("specialist_services_specialist_id_service_id_key", isUnique = true, specialistId, serviceId)
        index("idx_spec_serv_specialist", false, specialistId)
        index("idx_spec_serv_service", false, serviceId)
    }
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
    val specialistId = varchar("specialist_id", 50).references(SpecialistsTable.id, onDelete = ReferenceOption.CASCADE).index("idx_specialist_absences_specialist_id")
    val startTime = long("start_time")
    val endTime = long("end_time")
    val reason = text("reason").nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_specialist_absences_time", false, startTime, endTime)
        index("idx_specialist_absences_specialist_id", false, specialistId)
    }
}

object SpecialistClientNotesTable : Table("specialist_client_notes") {
    val id = varchar("id", 50)
    val specialistId = varchar("specialist_id", 50).references(SpecialistsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index("idx_spec_client_notes_user_id")
    val note = text("note")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_spec_client_notes_link", false, specialistId, userId)
    }
}

object AppointmentsTable : Table("appointments") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index("idx_appointments_user_id")
    val salonId = varchar("salon_id", 50).references(SalonsTable.id, onDelete = ReferenceOption.CASCADE).index("idx_appointments_salon_id")
    val specialistId = varchar("specialist_id", 50).references(SpecialistsTable.id, onDelete = ReferenceOption.CASCADE).index("idx_appointments_specialist_id")
    val status = varchar("status", 20).index("idx_appointments_status")
    val dateTime = long("date_time").index("idx_appointments_date_time")
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

    init {
        index("idx_appointments_specialist_time", false, specialistId, dateTime)
    }
}

object ProductCategoriesTable : Table("product_categories") {
    val id = varchar("id", 50)
    val name = varchar("name", 100).uniqueIndex()
    val imageUrl = varchar("image_url", 512).nullable()
    val description = text("description").nullable()

    override val primaryKey = PrimaryKey(id)
}

object ProductsTable : Table("products") {
    val id = varchar("id", 50)
    val name = varchar("name", 255)
    val category = varchar("category", 100).index("idx_products_category")
    val subCategory = varchar("sub_category", 100).nullable().index("idx_products_sub_category")
    val description = text("description")
    val imageUrl = varchar("image_url", 512).default("")
    val price = double("price")
    val discountPercent = integer("discount_percent").default(0)
    val stockQuantity = integer("stock_quantity").default(0)
    val isAvailable = bool("is_available").default(true)
    val gender = varchar("gender", 20).nullable().index("idx_products_gender")
    val providerName = varchar("provider_name", 100).default("Mira Store")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object OrdersTable : Table("orders") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index("idx_orders_user_id")
    val salonId = varchar("salon_id", 50).references(SalonsTable.id, onDelete = ReferenceOption.CASCADE)
    val subtotalAmount = double("subtotal_amount").default(0.0)
    val shippingFees = double("shipping_fees").default(0.0)
    val taxAmount = double("tax_amount").default(0.0)
    val totalAmount = double("total_amount")
    val discountAmount = double("discount_amount").default(0.0)
    val status = varchar("status", 50) // Matching DB
    val createdAt = long("created_at")
    val shippingAddress = text("shipping_address").nullable()
    val paymentMethod = varchar("payment_method", 100).nullable()
    val trackingNumber = varchar("tracking_number", 100).nullable()
    val specialInstructions = text("special_instructions").nullable()
    val promoCode = varchar("promo_code", 100).nullable()
    val idempotencyKey = varchar("idempotency_key", 100).nullable().uniqueIndex("orders_idempotency_key_key")
    val expiresAt = long("expires_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_orders_expiry", false, status, expiresAt)
    }
}

object OrderItemsTable : Table("order_items") {
    val id = varchar("id", 50)
    val orderId = varchar("order_id", 50).references(OrdersTable.id, onDelete = ReferenceOption.CASCADE).index("idx_order_items_order_id")
    val productId = varchar("product_id", 50).references(ProductsTable.id, onDelete = ReferenceOption.CASCADE)
    val quantity = integer("quantity")
    val price = double("price")
    val providerName = varchar("provider_name", 100).default("Mira Store")

    override val primaryKey = PrimaryKey(id)
}

object ReviewsTable : Table("reviews") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index("idx_reviews_user")
    val targetId = varchar("target_id", 50)
    val targetType = varchar("target_type", 20) // SPECIALIST, SERVICE, PRODUCT, APPOINTMENT
    val rating = integer("rating")
    val comment = text("comment").nullable()
    val imageUrl = varchar("image_url", 512).nullable()
    val adminReply = text("admin_reply").nullable()
    val adminReplyAt = long("admin_reply_at").nullable()
    val isVisible = bool("is_visible").default(true)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_reviews_target", false, targetId, targetType)
        index("idx_reviews_user_target", true, userId, targetId, targetType)
    }
}

object PromotionsTable : Table("promotions") {
    val id = varchar("id", 50)
    val code = varchar("code", 100).nullable().uniqueIndex("idx_promotions_code")
    val title = varchar("title", 255)
    val ctaText = varchar("cta_text", 100).nullable()
    val description = text("description")
    val discountType = varchar("discount_type", 50) 
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
    val status = varchar("status", 50).default("ACTIVE")
    val imageUrl = varchar("image_url", 512).nullable()
    val promoType = varchar("promo_type", 50).default("EXPERTS")
    val discountPercent = integer("discount_percent").default(0)
    val isActive = bool("is_active").default(true)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object PromotionUsagesTable : Table("promotion_usages") {
    val id = varchar("id", 50)
    val promotionId = varchar("promotion_id", 50).references(PromotionsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index("idx_promo_usages_user_id")
    val orderId = varchar("order_id", 50).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_promo_usages_user_promo", false, userId, promotionId)
    }
}

object OutboxTable : Table("outbox") {
    val eventId = varchar("event_id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val payload = text("payload")
    val createdAt = long("created_at")
    val dispatched = bool("dispatched").default(false)
    val isRead = bool("is_read").default(false)
    /**
     * Who this event is intended for:
     * - CLIENT: customer-facing notifications (iOS/Android apps).
     * - ADMIN: back-office events for the admin desktop dashboard only.
     */
    val audience = varchar("audience", 10).default(OutboxAudience.CLIENT.name)

    override val primaryKey = PrimaryKey(eventId)
}

enum class OutboxAudience { CLIENT, ADMIN }

object UserAddressesTable : Table("user_addresses") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index("idx_user_addresses_user_id")
    val firstName = varchar("first_name", 255)
    val lastName = varchar("last_name", 255)
    val label = varchar("label", 100)
    val line1 = varchar("line1", 255)
    val line2 = varchar("line2", 255).nullable()
    val postalCode = varchar("postal_code", 50)
    val city = varchar("city", 100)
    val state = varchar("state", 100).nullable()
    val country = varchar("country", 100).default("")
    val isDefault = bool("is_default").default(false)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object UserNotificationPreferencesTable : Table("user_notification_preferences") {
    val userId = varchar("user_id", 50).references(UsersTable.id, onDelete = ReferenceOption.CASCADE).index("idx_user_notif_pref_user_id")
    val pushEnabled = bool("push_enabled").default(true)
    val specialistMessagesEnabled = bool("specialist_messages_enabled").default(true)
    val bookingRemindersEnabled = bool("booking_reminders_enabled").default(true)
    val marketingEnabled = bool("marketing_enabled").default(false)
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(userId)
}

object MessagesTable : Table("messages") {
    val id = varchar("id", 50)
    val chatId = varchar("chat_id", 255).index("idx_messages_chat_id")
    val senderId = varchar("sender_id", 50)
    val recipientId = varchar("recipient_id", 50).nullable().index("idx_messages_recipient_id")
    val senderRole = varchar("sender_role", 20).default("CLIENT") // CLIENT, SPECIALIST, ADMIN
    val actingAsId = varchar("acting_as_id", 50).nullable() // Admin acting as Specialist
    val content = text("content")
    val status = varchar("status", 20).default("SENT") // SENT, DELIVERED, READ
    val isInternal = bool("is_internal").default(false) // Admin-only notes
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
